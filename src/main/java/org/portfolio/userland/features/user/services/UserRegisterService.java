package org.portfolio.userland.features.user.services;

import lombok.RequiredArgsConstructor;
import org.portfolio.userland.features.user.dto.common.EnFrontendFramework;
import org.portfolio.userland.features.user.dto.register.TokenActivateReq;
import org.portfolio.userland.features.user.dto.register.UserRegisterReq;
import org.portfolio.userland.features.user.entities.*;
import org.portfolio.userland.features.user.events.UserActivatedEvent;
import org.portfolio.userland.features.user.events.UserRegisteredEvent;
import org.portfolio.userland.features.user.exceptions.UserEmailAlreadyExistsException;
import org.portfolio.userland.features.user.mappers.UserRegisterMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Business logic for user registration and activate.
 * <p>User story:</p>
 * <ul>
 *   <li>User goes on user registration page and fills form.</li>
 *   <li>User clicks on registration button. Frontend calls <code>/api/users/register</code> endpoint.</li>
 *   <li>System creates pending user account, activate token and sends email with activate link. Note that link leads to frontend.</li>
 *   <li>User clicks on activate link, get redirected to frontend, frontend calls <code>/api/users/activate</code>.</li>
 *   <li>Frontend reacts appropriately to response from activate endpoint (show success or failure message).</li>
 *   <li>On successful activate, backend sends email confirming successful user account activate.</li>
 * </ul>
 * <p>Note we do not do anything beyond registration/activate itself here. We trigger events - other services (like
 * user email service sending activate email) will react to it.</p>
 */
@Service
@RequiredArgsConstructor
public class UserRegisterService extends BaseUserService {
  private final UserRegisterMapper userRegisterMapper;

  /** How long before activate token expires in hours. */
  @Value("${app.user.token.activation.expires}")
  private long activationTokenExpires;

  /**
   * Registers user in UserLand system.
   * @param userRegisterReq User registration request.
   * @return Created user.
   */
  @Transactional
  public User register(UserRegisterReq userRegisterReq) {
    LocalDateTime nowAt = clockService.getNowUTC();

    verifyRegistration(userRegisterReq);
    userRegisterReq = processRegistration(userRegisterReq);

    User user = createUserData(userRegisterReq, nowAt);
    user = userRepository.save(user);

    if (userRegisterReq.activate()) triggerActivationEvent(user, userRegisterReq.frontend());
    else triggerRegisterEvent(user, userRegisterReq);
    return user;
  }

  /**
   * Process registration.
   * @param userRegisterReq User registration request.
   * @return Modified user registration request.
   */
  private UserRegisterReq processRegistration(UserRegisterReq userRegisterReq) {
    Boolean activation = profile.getTest() ? userRegisterReq.activate() : false; // Never allow instant activate on PROD.
    return userRegisterReq.toBuilder().activate(activation).build();
  }

  /**
   * Verifies state of user, ensuring it is allowed to be registered in system.
   * @param userRegisterReq User registration request.
   */
  private void verifyRegistration(UserRegisterReq userRegisterReq) {
    if (userRepository.existsByEmail(userRegisterReq.email()))
      throw new UserEmailAlreadyExistsException(userRegisterReq.email());
  }

  //

  /**
   * Create and fill user data.
   * @param userRegisterReq User registration request.
   * @return User data.
   */
  private User createUserData(UserRegisterReq userRegisterReq, LocalDateTime nowAt) {
    User user = userRegisterMapper.toEntity(userRegisterReq);
    // Simple fields like status or blocked are pre-filled already.
    user.setCreatedAt(nowAt);
    user.setModifiedAt(nowAt);
    user.addHistory(createHistoryEvent(nowAt, EnUserHistoryWhat.CREATED));
    if (userRegisterReq.activate()) {
      user.setStatus(EnUserStatus.ACTIVE);
      user.addHistory(createHistoryEvent(nowAt, EnUserHistoryWhat.ACTIVATED));
    } else user.addToken(createTokenData(nowAt, EnUserTokenType.ACTIVATE));
    return user;
  }

  // //////////////////////////////////////////////////////////////////////////

  /**
   * Activate user that has token with given token string.
   * @param tokenActivateReq Token activate request.
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
    addHistoryEvent(user, nowAt, EnUserHistoryWhat.ACTIVATED);

    triggerActivationEvent(user, tokenActivateReq.frontend());
  }

  // //////////////////////////////////////////////////////////////////////////

  /**
   * Triggers user registration event for anyone interested.
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
    // Will trigger UserEmailService.sendActivationEmail().
    eventPublisher.publishEvent(userRegisteredEvent);
  }

  /**
   * Triggers user activate event for anyone interested.
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
    // Will trigger UserEmailService.sendActivationEmail().
    eventPublisher.publishEvent(userActivatedEvent);
  }
}
