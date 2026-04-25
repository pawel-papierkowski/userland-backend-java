package org.portfolio.userland.features.user.services;

import lombok.RequiredArgsConstructor;
import org.portfolio.userland.features.user.dto.register.TokenActivateReq;
import org.portfolio.userland.features.user.dto.register.UserRegisterReq;
import org.portfolio.userland.features.user.entities.*;
import org.portfolio.userland.features.user.events.UserActivatedEvent;
import org.portfolio.userland.features.user.events.UserRegisteredEvent;
import org.portfolio.userland.features.user.exceptions.UserEmailAlreadyExistsException;
import org.portfolio.userland.features.user.mapper.UserRegisterMapper;
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
 *   <li>Frontend reacts appropriately to response from activation endpoint (show success or failure message).</li>
 *   <li>On successful activation, backend sends email confirming successful user account activation.</li>
 * </ul>
 * <p>Note we do not do anything beyond registration/activation itself here. We trigger events - other services (like
 * user email service sending activation email) will react to it.</p>
 */
@Service
@RequiredArgsConstructor
public class UserRegisterService extends BaseUserService {
  private final UserRegisterMapper userRegisterMapper;

  /** How long before activation token expires in hours. */
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
    User user = createUserData(userRegisterReq, nowAt);
    user = userRepository.save(user);

    triggerRegisterEvent(userRegisterReq, user);
    return user;
  }

  /**
   * Verifies state of user, ensuring it is allowed to be registered in system.
   * @param userRegisterReq User registration request.
   */
  private void verifyRegistration(UserRegisterReq userRegisterReq) {
    if (userRepository.existsByEmail(userRegisterReq.email()))
      throw new UserEmailAlreadyExistsException(userRegisterReq.email());
  }

  /**
   * Triggers user registration event for anyone interested.
   * @param userRegisterReq User registration request.
   * @param user User data.
   */
  private void triggerRegisterEvent(UserRegisterReq userRegisterReq, User user) {
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
    user.addToken(createTokenData(nowAt, EnTokenType.ACTIVATE));
    user.addHistory(createHistoryEvent(nowAt, EnHistoryWhat.CREATED));
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

    UserToken userToken = resolveToken(nowAt, EnTokenType.ACTIVATE, tokenActivateReq.token());
    User user = userToken.getUser();
    user.setModifiedAt(nowAt);
    user.setStatus(EnUserStatus.ACTIVE);
    userRepository.save(user);

    userTokenRepository.deleteToken(userToken.getToken());
    addHistoryEvent(user, nowAt, EnHistoryWhat.ACTIVATED);

    triggerActivationEvent(tokenActivateReq, user);
  }

  /**
   * Triggers user activation event for anyone interested.
   * @param tokenActivateReq Token activation request.
   * @param user User data.
   */
  private void triggerActivationEvent(TokenActivateReq tokenActivateReq, User user) {
    UserActivatedEvent userActivatedEvent = new UserActivatedEvent(
        user.getId(),
        user.getUsername(),
        user.getEmail(),
        user.getLang(),
        tokenActivateReq.frontend()
    );
    // Will trigger UserEmailService.sendActivationEmail().
    eventPublisher.publishEvent(userActivatedEvent);
  }
}
