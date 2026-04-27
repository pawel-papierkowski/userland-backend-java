package org.portfolio.userland.features.user.services;

import org.portfolio.userland.features.user.entities.*;
import org.portfolio.userland.features.user.exceptions.*;
import org.portfolio.userland.features.user.repositories.UserHistoryRepository;
import org.portfolio.userland.features.user.repositories.UserJwtRepository;
import org.portfolio.userland.features.user.repositories.UserRepository;
import org.portfolio.userland.features.user.repositories.UserTokenRepository;
import org.portfolio.userland.system.BaseService;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Base for all user services.
 */
public abstract class BaseUserService extends BaseService {
  @Autowired
  private UserHelperService userHelperService;

  @Autowired
  protected UserRepository userRepository;
  @Autowired
  protected UserHistoryRepository userHistoryRepository;
  @Autowired
  protected UserTokenRepository userTokenRepository;
  @Autowired
  protected UserJwtRepository userJwtRepository;

  //

  /**
   * Resolves user and verifies user state.
   * @param email User email.
   * @return User.
   */
  protected User resolveUser(String email) {
    User user = userRepository.findByEmail(email).orElseThrow(() -> new UserDoesNotExistException(email));
    if (EnUserStatus.PENDING.equals(user.getStatus())) throw new UserMustBeActiveException(email);
    if (user.getLocked()) throw new UserCannotBeLockedException(email);
    return user;
  }

  //

  /**
   * Add token entry to user.
   * @param user User.
   * @param nowAt Current date&time.
   * @param type Type of token.
   */
  protected void addTokenEntry(User user, LocalDateTime nowAt, EnUserTokenType type) {
    UserToken userToken = createTokenData(nowAt, type);
    userToken.setUser(user);
    userTokenRepository.save(userToken);
  }

  /**
   * Create and fill token data.
   * @param nowAt Current date&time.
   * @param type Type of token.
   * @return User token entry.
   */
  protected UserToken createTokenData(LocalDateTime nowAt, EnUserTokenType type) {
    UserToken token = new UserToken();
    token.setCreatedAt(nowAt);
    token.setExpiresAt(userHelperService.resolveExpiration(nowAt, type));
    token.setType(type);
    token.setToken(securityGeneratorService.token());
    return token;
  }

  /**
   * Ensures given token does not exist. If valid token of given type already exists for given user, throws exception.
   * @param nowAt Current date&time.
   * @param type Type of token.
   * @param user User.
   */
  protected void verifyTokenDoesNotExist(LocalDateTime nowAt, EnUserTokenType type, User user) {
    List<UserToken> tokens = user.getTokens();
    UserToken token = null;
    for (UserToken currToken : tokens) {
      if (type.equals(currToken.getType())) {
        token = currToken;
        break;
      }
    }
    if (token == null) return; // no token of this type present at all, everything is fine

    // Expired token will be removed to make place for new token.
    if (token.getExpiresAt().isBefore(nowAt)) {
      tokens.remove(token);
      userRepository.saveAndFlush(user); // important to flush here, otherwise Bad Things Happen
      return;
    }

    // Token still valid, throw exception.
    throw new UserTokenAlreadyExistsException(type);
  }

  /**
   * Retrieve user token based on token string. Will throw exception if token is not found or is expired.
   * @param tokenStr Token string.
   */
  protected UserToken resolveToken(LocalDateTime nowAt, EnUserTokenType type, String tokenStr) {
    UserToken userToken = userTokenRepository.findByTypeAndToken(type, tokenStr)
        .orElseThrow(() -> new UserTokenMissingException(tokenStr));
    if (userToken.getExpiresAt().isBefore(nowAt)) throw new UserTokenExpiredException(tokenStr);
    return userToken;
  }

  //

  /**
   * Add history event to user.
   * @param user User.
   * @param nowAt Current date&time.
   * @param what What happened.
   */
  protected void addHistoryEvent(User user, LocalDateTime nowAt, EnUserHistoryWhat what) {
    UserHistory historyEvent = createHistoryEvent(nowAt, what);
    historyEvent.setUser(user);
    userHistoryRepository.save(historyEvent);
  }

  /**
   * Create and fill history event.
   * @param nowAt Current date&time.
   * @param what What happened.
   * @return User history event.
   */
  protected UserHistory createHistoryEvent(LocalDateTime nowAt, EnUserHistoryWhat what) {
    UserHistory event = new UserHistory();
    event.setUuid(securityGeneratorService.uuid());
    event.setCreatedAt(nowAt);
    event.setWho(EnUserHistoryWho.USER);
    event.setWhat(what);
    return event;
  }

  //

  /**
   * Add JWT entry to user.
   * @param user User.
   * @param nowAt Current date&time.
   * @param jwtStr JWT string.
   */
  protected void addJwtEntry(User user, LocalDateTime nowAt, String jwtStr) {
    UserJwt jwtEntry = createJwtEntry(nowAt, jwtStr);
    jwtEntry.setUser(user);
    userJwtRepository.save(jwtEntry);
  }

  /**
   * Create and fill JWT data.
   * @param nowAt Current date&time.
   * @param jwtStr JWT string.
   * @return User JWT entry.
   */
  protected UserJwt createJwtEntry(LocalDateTime nowAt, String jwtStr) {
    UserJwt token = new UserJwt();
    token.setCreatedAt(nowAt);
    token.setExpiresAt(userHelperService.resolveJwtExpiration(nowAt));
    token.setToken(jwtStr);
    return token;
  }
}
