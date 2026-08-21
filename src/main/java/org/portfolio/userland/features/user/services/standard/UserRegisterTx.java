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
import org.portfolio.userland.features.user.services.BaseUserService;
import org.portfolio.userland.system.auth.perm.PermConst;
import org.springframework.beans.factory.annotation.Value;
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
   * @param userRegisterReq User registration request.
   * @param passwordHash Already computed BCrypt hash of user's password.
   */
  public void register(UserRegisterReq userRegisterReq, String passwordHash) {
    // We need to react properly in case there is already user with given email in system.
    // On production, we cannot return error as it would allow email enumeration attack.
    boolean alreadyRegistered = userRepository.existsByEmail(userRegisterReq.email());
    if (alreadyRegistered) alreadyRegistered(userRegisterReq);
    else actuallyRegister(userRegisterReq, passwordHash);
  }

  /**
   * Act in case user is already registered.
   * @param userRegisterReq User registration request.
   */
  private void alreadyRegistered(UserRegisterReq userRegisterReq) {
    log.trace("User '{}' is already registered.", userRegisterReq.email());

    User user = userHelperService.resolveUser(userRegisterReq.email(), true);
    if (user == null) return; // should not happen
    triggerAlreadyRegisteredEvent(user, userRegisterReq.frontend());

    // On production, we will pretend everything is fine and dandy.
  }

  /**
   * Register new user.
   * @param userRegisterReq User registration request.
   * @param passwordHash Already computed BCrypt hash of user's password.
   */
  private void actuallyRegister(UserRegisterReq userRegisterReq, String passwordHash) {
    LocalDateTime nowAt = clockService.getNowUTC();

    User user = createUserData(userRegisterReq, nowAt, passwordHash);
    user = userRepository.save(user);
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
    if (userRegisterReq.activate()) { // already activate user?
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
    UserRegisteredEvent userRegisteredEvent = new UserRegisteredEvent(
        user.getId(),
        user.getUsername(),
        user.getEmail(),
        user.getLang(),
        userRegisterReq.frontend(),
        user.getTokens().getFirst().getToken(),
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
