package org.portfolio.userland.features.user.services;

import org.portfolio.userland.features.user.entities.*;
import org.portfolio.userland.features.user.exceptions.UserTokenAlreadyExistsException;
import org.portfolio.userland.features.user.exceptions.UserTokenAlreadyUsedException;
import org.portfolio.userland.features.user.exceptions.UserTokenExpiredException;
import org.portfolio.userland.features.user.exceptions.UserTokenMissingException;
import org.portfolio.userland.features.user.mappers.UserMapper;
import org.portfolio.userland.features.user.repositories.config.UserConfigRepository;
import org.portfolio.userland.features.user.repositories.history.UserHistoryRepository;
import org.portfolio.userland.features.user.repositories.jwt.UserJwtRepository;
import org.portfolio.userland.features.user.repositories.permission.PermissionRepository;
import org.portfolio.userland.features.user.repositories.permission.UserPermissionRepository;
import org.portfolio.userland.features.user.repositories.token.UserTokenRepository;
import org.portfolio.userland.features.user.repositories.user.UserProfileRepository;
import org.portfolio.userland.features.user.repositories.user.UserRepository;
import org.portfolio.userland.system.base.BaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Base for all user services.
 */
public abstract class BaseUserService extends BaseService {
  @Autowired
  protected UserRepository userRepository;
  @Autowired
  protected UserProfileRepository userProfileRepository;
  @Autowired
  protected UserConfigRepository userConfigRepository;
  @Autowired
  protected UserHistoryRepository userHistoryRepository;
  @Autowired
  protected UserTokenRepository userTokenRepository;
  @Autowired
  protected UserJwtRepository userJwtRepository;
  @Autowired
  protected UserPermissionRepository userPermissionRepository;
  @Autowired
  protected PermissionRepository permissionRepository;

  @Autowired
  protected UserMapper userMapper;

  //

  /**
   * Create token entry and persist it atomically, guarding against concurrent creation of same token type for same
   * user. If another transaction created such token in the meantime, throws {@link UserTokenAlreadyExistsException}.
   * <p>Note: the real guard is the unique constraint <code>uq_user_token_type</code>, so this method is race-safe even
   * though {@link #ensureTokenDoesNotExist} (fast-path pre-check) alone is not.</p>
   * <p>Warning: on constraint violation the current transaction must be aborted immediately (persistence context is
   * inconsistent), which this method does by throwing - rollback also discards history events and any AFTER_COMMIT
   * email events of the losing transaction. Callers that must react gracefully (e.g. fail silently) have to catch
   * this exception <em>outside</em> of the transactional method.</p>
   * @param user User.
   * @param nowAt Current date&time.
   * @param type Type of token.
   * @return Persisted token entry.
   */
  protected UserToken persistNewToken(User user, LocalDateTime nowAt, EnUserTokenType type) {
    return persistNewToken(user, nowAt, type, null);
  }

  /**
   * Same as {@link #persistNewToken(User, LocalDateTime, EnUserTokenType)}, but with payload.
   * @param user User.
   * @param nowAt Current date&time.
   * @param type Type of token.
   * @param payload Payload of token.
   * @return Persisted token entry.
   */
  protected UserToken persistNewToken(User user, LocalDateTime nowAt, EnUserTokenType type, String payload) {
    UserToken token = createTokenData(nowAt, type, payload);
    token.setUser(user);
    try {
      userTokenRepository.save(token);
      // Flush immediately as well - save() alone does not guarantee the INSERT was validated yet.
      userTokenRepository.flush();
    } catch (DataIntegrityViolationException ex) {
      // We lost the race: another concurrent transaction created a token of this type for this user first.
      // Note: since UserToken uses IDENTITY id, the INSERT typically already executes during save(), so the
      // violation surfaces there; flush() covers the deferred case. Transaction must be aborted now
      // (persistence context is inconsistent), which throwing accomplishes - rollback also discards history
      // events and any AFTER_COMMIT email events of the losing transaction.
      throw new UserTokenAlreadyExistsException(type);
    }
    return token;
  }

  /**
   * Create and fill token data.
   * @param nowAt Current date&time.
   * @param type Type of token.
   * @return User token entry.
   */
  protected UserToken createTokenData(LocalDateTime nowAt, EnUserTokenType type) {
    return createTokenData(nowAt, type, null);
  }

  /**
   * Create and fill token data, including payload.
   * @param nowAt Current date&time.
   * @param type Type of token.
   * @param payload Payload of token.
   * @return User token entry.
   */
  protected UserToken createTokenData(LocalDateTime nowAt, EnUserTokenType type, String payload) {
    UserToken token = new UserToken();
    // Note createdAt is maintained automatically by JPA auditing.
    token.setExpiresAt(userHelperService.resolveExpirationSince(nowAt, type));
    token.setType(type);
    token.setToken(securityGeneratorService.token());
    token.setPayload(payload);
    return token;
  }

  /**
   * Ensures token of given type for given user does not exist. If token exists, but is expired, it will be removed.
   * If token exists and is still valid, throws exception.
   * <p>Reminder: one user can have only one token of given type at once.</p>
   * <p>Note: token is fetched directly from database instead of traversing user's lazy 'tokens' collection,
   * so no unnecessary data is loaded.</p>
   * <p>Note: this is just a fast-path check to fail early with a nice error message - it does not guarantee
   * uniqueness under concurrency (two transactions can both pass it). The real guard is the unique constraint
   * <code>uq_user_token_type</code>, enforced atomically by {@link #persistNewToken}.</p>
   * @param nowAt Current date&time.
   * @param type  Type of token.
   * @param user  User.
   */
  protected void ensureTokenDoesNotExist(LocalDateTime nowAt, EnUserTokenType type, User user) {
    Optional<UserToken> found = userTokenRepository.findByUserAndType(user.getId(), type);
    if (found.isEmpty()) return; // no token of this type present at all, everything is fine
    UserToken token = found.get();

    // Expired token will be removed to make place for new token. Note orphan removal is not used, so we delete it explicitly.
    if (token.getExpiresAt().isBefore(nowAt)) {
      userTokenRepository.delete(token);
      // Important to flush here, otherwise Bad Things Happen (inserting new token of same type later in same transaction
      // would violate unique constraint uq_user_token_type). It is fine if it is saved in rollback scenario, as
      // expired tokens cannot be used anyway.
      userTokenRepository.flush();
      return;
    }

    throw new UserTokenAlreadyExistsException(type);
  }

  /**
   * Retrieve user token based on token string and atomically consume it, so it cannot be used again.
   * Will throw exception if token is not found, is expired or was already used by another (concurrent) request.
   * <p>Note: consumption is done via atomic conditional delete, so even if multiple requests race with the same
   * token, exactly one of them will win here - all others will fail and will not perform their action.
   * The returned token entry is already deleted in the database (but still usable as read-only data).</p>
   * @param nowAt Current date&time.
   * @param type Type of token.
   * @param tokenStr Token string.
   * @return User token entry (already consumed in database).
   */
  protected UserToken resolveToken(LocalDateTime nowAt, EnUserTokenType type, String tokenStr) {
    UserToken userToken = userTokenRepository.findByTypeAndToken(type, tokenStr)
        .orElseThrow(() -> new UserTokenMissingException(tokenStr));
    if (userToken.getExpiresAt().isBefore(nowAt)) throw new UserTokenExpiredException(tokenStr);

    // Atomically claim the token: if another concurrent transaction consumed it in the meantime,
    // nothing will be deleted here and this request must not perform its action.
    int consumed = userTokenRepository.consumeToken(type, tokenStr, nowAt);
    if (consumed == 0) throw new UserTokenAlreadyUsedException(tokenStr);
    return userToken;
  }

  //

  /**
   * Add history event to user. Note it persists event.
   * @param user   User.
   * @param nowAt  Current date&time.
   * @param who    Who caused that event.
   * @param what   What happened.
   * @param params Event parameters.
   */
  protected void addHistoryEvent(User user, LocalDateTime nowAt, EnUserHistoryWho who, EnUserHistoryWhat what, String params) {
    UserHistory historyEvent = createHistoryEvent(nowAt, who, what, params);
    historyEvent.setUser(user);
    userHistoryRepository.save(historyEvent);
  }

  /**
   * Add history event to user. Note it persists event.
   * @param userId User id.
   * @param nowAt  Current date&time.
   * @param who    Who caused that event.
   * @param what   What happened.
   * @param params Event parameters.
   */
  protected void addHistoryEvent(Long userId, LocalDateTime nowAt, EnUserHistoryWho who, EnUserHistoryWhat what, String params) {
    UserHistory historyEvent = createHistoryEvent(nowAt, who, what, params);
    historyEvent.setUser(userRepository.getReferenceById(userId));
    userHistoryRepository.save(historyEvent);
  }

  /**
   * Create and fill history event. It does NOT persist event.
   * @param nowAt  Current date&time.
   * @param who    Who caused that event.
   * @param what   What happened.
   * @param params Event parameters.
   * @return User history event.
   */
  protected UserHistory createHistoryEvent(LocalDateTime nowAt, EnUserHistoryWho who, EnUserHistoryWhat what, String params) {
    UserHistory event = new UserHistory();
    event.setUuid(securityGeneratorService.uuid());
    // Note createdAt is maintained automatically by JPA auditing.
    event.setWho(who);
    event.setWhat(what);
    event.setParams(params);
    return event;
  }

  //

  /**
   * Add JWT entry to user and save it.
   * @param user User.
   * @param nowAt Current date&time.
   * @param customExpiration Custom expiration in minutes. Can be null, will use default expiration.
   * @param jwtStr JWT string.
   */
  protected void addJwtEntry(User user, LocalDateTime nowAt, String jwtStr, Long customExpiration) {
    UserJwt jwtEntry = createJwtEntry(nowAt, jwtStr, customExpiration);
    jwtEntry.setUser(user);
    userJwtRepository.save(jwtEntry);
  }

  /**
   * Create and fill JWT data.
   * @param nowAt Current date&time.
   * @param jwtStr JWT string.
   * @param customExpiration Custom expiration in minutes. Can be null, will use default expiration.
   * @return User JWT entry.
   */
  private UserJwt createJwtEntry(LocalDateTime nowAt, String jwtStr, Long customExpiration) {
    UserJwt token = new UserJwt();
    // Note createdAt is maintained automatically by JPA auditing.
    token.setExpiresAt(userHelperService.resolveJwtExpiration(nowAt, customExpiration));
    token.setToken(jwtStr);
    return token;
  }

  //

  /**
   * Add permission entry to user and save it.
   * @param user User.
   * @param name Name of permission.
   * @param value Value of permission.
   */
  protected void addPermission(User user, String name, String value) {
    UserPermission permissionEntry = createPermission(name, value);
    permissionEntry.setUser(user);
    userPermissionRepository.save(permissionEntry);
  }

  /**
   * Create and fill permission data.
   * @param name Name of permission.
   * @param value Value of permission.
   * @return User permission entry.
   */
  protected UserPermission createPermission(String name, String value) {
    Permission permission = permissionRepository.findByName(name).orElseThrow();

    UserPermission permissionEntry = new UserPermission();
    permissionEntry.setUuid(securityGeneratorService.uuid());
    // Note createdAt is maintained automatically by JPA auditing.
    permissionEntry.setPermission(permission);
    permissionEntry.setValue(value);
    return permissionEntry;
  }
}
