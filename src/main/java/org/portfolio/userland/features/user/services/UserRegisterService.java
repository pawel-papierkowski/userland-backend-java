package org.portfolio.userland.features.user.services;

import lombok.RequiredArgsConstructor;
import org.portfolio.userland.features.user.dto.common.EnFrontendFramework;
import org.portfolio.userland.features.user.dto.register.TokenActivateReq;
import org.portfolio.userland.features.user.dto.register.UserRegisterReq;
import org.portfolio.userland.features.user.entities.*;
import org.portfolio.userland.features.user.events.UserActivatedEvent;
import org.portfolio.userland.features.user.events.UserAlreadyRegisteredEvent;
import org.portfolio.userland.features.user.events.UserRegisteredEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Business logic for user registration and activation.
 * <p>User story:</p>
 * <ul>
 *   <li>User goes on user registration page and fills form.</li>
 *   <li>User clicks on registration button. Frontend calls <code>/api/users/register</code> endpoint.</li>
 *   <li>System creates pending user account, activation token and sends email with activation link. Note that link leads to frontend.</li>
 *   <li>User clicks on activation link, get redirected to frontend, frontend calls <code>/api/users/activate</code>.</li>
 *   <li>Frontend reacts appropriately to response from <code>/api/users/activate</code> endpoint (show success or failure message).</li>
 *   <li>On successful activation, backend sends email confirming successful user account activation.</li>
 * </ul>
 * <p>Note we do not do anything beyond registration/activation itself here. We trigger events - other services (like
 * user email service sending registration email) will react to it.</p>
 */
@Service
@RequiredArgsConstructor
public class UserRegisterService extends BaseUserService {
  /** How long before activation token expires in hours. */
  @Value("${app.user.token.activation.expires}")
  private long activationTokenExpires;

  /**
   * Registers user in UserLand system.
   * @param userRegisterReq User registration request.
   */
  @Transactional
  public void register(UserRegisterReq userRegisterReq) {
    // We need to react properly in case there is already user with given email in system.
    // We cannot return error as it would allow email enumeration attack.
    boolean alreadyRegistered = userRepository.existsByEmail(userRegisterReq.email());
    if (alreadyRegistered) alreadyRegistered(userRegisterReq);
    else actuallyRegister(userRegisterReq);
  }

  /**
   * Register new user.
   * @param userRegisterReq User registration request.
   */
  private void actuallyRegister(UserRegisterReq userRegisterReq) {
    LocalDateTime nowAt = clockService.getNowUTC();
    userRegisterReq = modifyRegistrationReq(userRegisterReq);

    User user = createUserData(userRegisterReq, nowAt);
    user = userRepository.save(user);
    UserProfile userProfile = new UserProfile();
    userProfile.setUser(user);
    userProfileRepository.save(userProfile);

    if (userRegisterReq.activate()) triggerActivationEvent(user, userRegisterReq.frontend());
    else triggerRegisterEvent(user, userRegisterReq);
  }

  /**
   * Act in case user is already registered.
   * @param userRegisterReq User registration request.
   */
  private void alreadyRegistered(UserRegisterReq userRegisterReq) {
    User user = userHelperService.resolveUser(userRegisterReq.email(), true);
    if (user == null) return; // should not happen
    triggerAlreadyRegisteredEvent(user, userRegisterReq.frontend());
  }

  /**
   * Modify registration request.
   * @param userRegisterReq User registration request.
   * @return Modified user registration request.
   */
  private UserRegisterReq modifyRegistrationReq(UserRegisterReq userRegisterReq) {
    // Never allow user activation on spot during registration on PROD. This is convenience option for testing during development.
    Boolean activate = profile.getTest() ? userRegisterReq.activate() : false;
    return userRegisterReq.toBuilder().activate(activate).build();
  }

  //

  /**
   * Create and fill user data.
   * @param userRegisterReq User registration request.
   * @return User data.
   */
  private User createUserData(UserRegisterReq userRegisterReq, LocalDateTime nowAt) {
    User user = userMapper.registerReqToUser(userRegisterReq);
    // Simple fields like status or blocked are pre-filled already.
    user.setCreatedAt(nowAt);
    user.setModifiedAt(nowAt);
    user.addHistory(createHistoryEvent(nowAt, EnUserHistoryWhat.CREATE, ""));
    if (userRegisterReq.activate()) {
      user.setStatus(EnUserStatus.ACTIVE);
      user.addHistory(createHistoryEvent(nowAt, EnUserHistoryWhat.ACTIVATE, ""));
    } else user.addToken(createTokenData(nowAt, EnUserTokenType.ACTIVATE));
    return user;
  }

  // //////////////////////////////////////////////////////////////////////////

  /**
   * Activate user that has token with given token string.
   * @param tokenActivateReq Token activation request.
   */
  @Transactional
  public void activate(TokenActivateReq tokenActivateReq) {
    LocalDateTime nowAt = clockService.getNowUTC();

    UserToken userToken = resolveToken(nowAt, EnUserTokenType.ACTIVATE, tokenActivateReq.token());
    User user = userToken.getUser();
    user.setModifiedAt(nowAt);
    user.setStatus(EnUserStatus.ACTIVE);
    userRepository.save(user);

    userTokenRepository.deleteToken(userToken.getToken());
    addHistoryEvent(user, nowAt, EnUserHistoryWhat.ACTIVATE, "");

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
    // Will trigger UserEmailService.sendRegistrationEmail().
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
    // Will trigger UserEmailService.sendActivatedEmail().
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
    // Will trigger UserEmailService.sendAlreadyRegisteredEmail().
    eventPublisher.publishEvent(userAlreadyRegisteredEvent);
  }
}
