package org.portfolio.userland.features.user.services.standard;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.portfolio.userland.features.user.dto.common.EnFrontendFramework;
import org.portfolio.userland.features.user.dto.standard.register.TokenActivateReq;
import org.portfolio.userland.features.user.dto.standard.register.UserRegisterReq;
import org.portfolio.userland.features.user.entities.*;
import org.portfolio.userland.features.user.events.UserActivatedEvent;
import org.portfolio.userland.features.user.events.UserAlreadyRegisteredEvent;
import org.portfolio.userland.features.user.events.UserRegisteredEvent;
import org.portfolio.userland.features.user.exceptions.UserAlreadyRegisteredException;
import org.portfolio.userland.features.user.exceptions.UserTokenNotFoundException;
import org.portfolio.userland.features.user.services.BaseUserService;
import org.portfolio.userland.system.auth.perm.PermConst;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Transactional part of user registration and activation.
 * <p>It is separated from {@link UserRegisterService} so expensive CPU operations (BCrypt password hashing) can run
 * outside of transaction and not hold a database connection.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserRegisterTx extends BaseUserService {
  /** How long before activation token expires in hours. */
  @Value("${app.user.token.activation.expires}")
  private long activationTokenExpires;

  /**
   * Registers user in system. Password hash must be already computed (see {@link UserRegisterService#register}).
   * <p>Note: if registration loses a race against a concurrent registration with the same email, throws
   * {@link UserAlreadyRegisteredException} - the caller (non-transactional orchestrator) must then run the graceful
   * "already registered" flow in a fresh transaction.</p>
   * @param userRegisterReq User registration request.
   * @param passwordHash Already computed BCrypt hash of user's password.
   */
  public void register(UserRegisterReq userRegisterReq, String passwordHash) {
    // We need to react properly in case there is already user with given email in system.
    // On production, we cannot return error as it would allow email enumeration attack.
    // Note: this is just a fast-path check - the real guard is the unique constraint on users.email,
    // enforced atomically below via catch of DataIntegrityViolationException.
    boolean alreadyRegistered = userRepository.existsByEmail(userRegisterReq.email());
    if (alreadyRegistered) alreadyRegistered(userRegisterReq);
    else actuallyRegister(userRegisterReq, passwordHash);
  }

  /**
   * Act in case user is already registered. Sends informational email to given address (and pretends success on API).
   * Must run in its own fresh transaction - it is also used as graceful fallback when registration lost a race against
   * concurrent registration with same email (see {@link UserRegisterService#register}).
   * @param userRegisterReq User registration request.
   */
  public void alreadyRegistered(UserRegisterReq userRegisterReq) {
    log.trace("User '{}' is already registered.", userRegisterReq.email());

    User user = userHelperService.resolveUser(userRegisterReq.email(), true);
    if (user == null) return; // should not happen
    triggerAlreadyRegisteredEvent(user, userRegisterReq.frontend());

    // On production, we will pretend everything is fine and dandy.
  }

  /**
   * Register new user.
   * <p>Note: the real guard against duplicate emails is the unique constraint on <code>users.email</code>. On
   * violation (concurrent registration with same email won the race) throws {@link UserAlreadyRegisteredException} -
   * transaction must be aborted, so the caller handles the graceful flow in a fresh transaction.</p>
   * @param userRegisterReq User registration request.
   * @param passwordHash Already computed BCrypt hash of user's password.
   */
  private void actuallyRegister(UserRegisterReq userRegisterReq, String passwordHash) {
    LocalDateTime nowAt = clockService.getNowUTC();

    User user = createUserData(userRegisterReq, nowAt, passwordHash);
    try {
      // Note: since User uses IDENTITY id, INSERT executes already during save(), so a violation of the unique
      // email constraint surfaces here rather than at commit time, where it could not be translated anymore.
      userRepository.save(user);
    } catch (DataIntegrityViolationException ex) {
      // We lost the race: another concurrent transaction registered user with this email first.
      throw new UserAlreadyRegisteredException(userRegisterReq.email());
    }
    UserProfile userProfile = createUserProfileData(userRegisterReq, user);
    userProfileRepository.save(userProfile);

    if (userRegisterReq.activate()) {
      log.trace("User '{}' registered and activated successfully.", userRegisterReq.email());
      triggerActivationEvent(user, userRegisterReq.frontend());
    } else {
      log.trace("User '{}' registered successfully.", userRegisterReq.email());
      triggerRegisterEvent(user, userRegisterReq);
    }
  }

  /**
   * Create and fill user data.
   * @param userRegisterReq User registration request.
   * @param nowAt Current date&time.
   * @param passwordHash Already computed BCrypt hash of user's password.
   * @return User data.
   */
  private User createUserData(UserRegisterReq userRegisterReq, LocalDateTime nowAt, String passwordHash) {
    User user = userMapper.registerReqToUser(userRegisterReq);
    // Simple fields like status or blocked are pre-filled already.
    // Note: mapper does NOT set password, hashing has to happen outside of transaction (CPU-heavy BCrypt).
    user.setPassword(passwordHash);
    user.setUuid(securityGeneratorService.uuid());
    // createdAt and modifiedAt is maintained automatically by JPA auditing
    user.addHistory(createHistoryEvent(nowAt, EnUserHistoryWho.USER, EnUserHistoryWhat.CREATE, ""));
    if (userRegisterReq.activate()) { // Already activate user? This will skip email confirmation.
      user.setStatus(EnUserStatus.ACTIVE);
      user.addHistory(createHistoryEvent(nowAt, EnUserHistoryWho.USER, EnUserHistoryWhat.ACTIVATE, ""));
    } else user.addToken(createTokenData(nowAt, EnUserTokenType.ACTIVATE));

    if (userRegisterReq.isAdmin()) user.addPermission(createPermission(PermConst.ROLE, PermConst.ROLE_ADMIN));
    return user;
  }

  /**
   * Create and fill user profile data.
   * @param userRegisterReq User registration request.
   * @param user User data.
   * @return User profile data.
   */
  private UserProfile createUserProfileData(UserRegisterReq userRegisterReq, User user) {
    UserProfile userProfile = new UserProfile();
    userProfile.setUser(user);
    userProfile.setName(userRegisterReq.name());
    userProfile.setSurname(userRegisterReq.surname());
    return userProfile;
  }

  // //////////////////////////////////////////////////////////////////////////

  /**
   * Activate user that has token with given token string.
   * @param tokenActivateReq Token activation request.
   */
  public void activate(TokenActivateReq tokenActivateReq) {
    LocalDateTime nowAt = clockService.getNowUTC();

    UserToken userToken = resolveToken(nowAt, EnUserTokenType.ACTIVATE, tokenActivateReq.token());
    User user = userToken.getUser();
    user.setStatus(EnUserStatus.ACTIVE);
    userRepository.save(user); // modifiedAt is maintained automatically by JPA auditing

    addHistoryEvent(user, nowAt, EnUserHistoryWho.USER, EnUserHistoryWhat.ACTIVATE, "");

    log.trace("User '{}' activated successfully.", user.getEmail());

    triggerActivationEvent(user, tokenActivateReq.frontend());
  }

  // //////////////////////////////////////////////////////////////////////////

  /**
   * Triggers user registered event for anyone interested.
   * @param user User data.
   * @param userRegisterReq User registration request.
   */
  private void triggerRegisterEvent(User user, UserRegisterReq userRegisterReq) {
    UserToken token = user.findToken(EnUserTokenType.ACTIVATE);
    if (token == null) throw new UserTokenNotFoundException();

    UserRegisteredEvent userRegisteredEvent = new UserRegisteredEvent(
        user.getId(),
        user.getUsername(),
        user.getEmail(),
        user.getLang(),
        userRegisterReq.frontend(),
        token.getToken(),
        activationTokenExpires
    );
    // Will trigger UserSendEmailService.sendRegistrationEmail().
    eventPublisher.publishEvent(userRegisteredEvent);
  }

  /**
   * Triggers user activated event for anyone interested.
   * @param user User data.
   * @param frontend Frontend.
   */
  private void triggerActivationEvent(User user, EnFrontendFramework frontend) {
    UserActivatedEvent userActivatedEvent = new UserActivatedEvent(
        user.getId(),
        user.getUsername(),
        user.getEmail(),
        user.getLang(),
        frontend
    );
    // Will trigger UserSendEmailService.sendActivatedEmail().
    eventPublisher.publishEvent(userActivatedEvent);
  }

  /**
   * Triggers user already registered event for anyone interested.
   * @param user User data.
   * @param frontend Frontend.
   */
  private void triggerAlreadyRegisteredEvent(User user, EnFrontendFramework frontend) {
    UserAlreadyRegisteredEvent userAlreadyRegisteredEvent = new UserAlreadyRegisteredEvent(
        user.getId(),
        user.getUsername(),
        user.getEmail(),
        user.getLang(),
        frontend
    );
    // Will trigger UserSendEmailService.sendAlreadyRegisteredEmail().
    eventPublisher.publishEvent(userAlreadyRegisteredEvent);
  }
}
