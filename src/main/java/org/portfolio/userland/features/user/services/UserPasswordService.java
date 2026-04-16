package org.portfolio.userland.features.user.services;

import lombok.RequiredArgsConstructor;
import org.portfolio.userland.features.user.data.*;
import org.portfolio.userland.features.user.dto.password.UserPassResetReq;
import org.portfolio.userland.features.user.dto.password.UserPassSendReq;
import org.portfolio.userland.features.user.events.UserPasswordResetConfirmEvent;
import org.portfolio.userland.features.user.events.UserPasswordResetSendEvent;
import org.portfolio.userland.features.user.exception.UserCannotBeLockedException;
import org.portfolio.userland.features.user.exception.UserDoesNotExistException;
import org.portfolio.userland.features.user.exception.UserMustBeActiveException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Business logic for user password reset.
 * <p>User story:</p>
 * <ul>
 *   <li>User on frontend clicks "I forgot password" option and is redirected to password reset form.</li>
 *   <li>User fills form (email) and clicks on button. Frontend calls <code>/api/users/password/send</code> endpoint.</li>
 *   <li>System creates password reset token and sends password reset email with link to frontend.</li>
 *   <li>User clicks on link, get redirected to separate page where they can enter new password.</li>
 *   <li>User clicks on reset password button. Frontend calls <code>/api/users/password/reset</code>.</li>
 *   <li>Backend verifies call and in case of success changes password to new.</li>
 *   <li>Frontend reacts appropriately to response from password reset endpoint (show success or failure message).</li>
 *   <li>Backend sends email confirming successful password reset.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class UserPasswordService extends BaseUserService {
  private final PasswordEncoder passwordEncoder;

  /** How long before password reset token expires in minutes. */
  @Value("${app.user.token.passwordReset.expires}")
  private long passwordResetTokenExpires;

  /**
   * Creates password reset token and (indirectly, via event) sends email with password reset link to user with given
   * email.
   * @param userPassSendReq User password reset send request.
   */
  @Transactional
  public void send(UserPassSendReq userPassSendReq) {
    User user = resolveUser(userPassSendReq);

    LocalDateTime nowAt = clockService.getNowUTC();
    UserToken token = createTokenData(nowAt, EnTokenType.PASSWORD);
    user.addToken(token);
    user.addHistory(createHistoryEvent(nowAt, EnHistoryWhat.PASS_RESET_REQ));
    user = userRepository.save(user);

    triggerPassSendEvent(user, token);
  }

  /**
   * Resolves and verifies user state.
   * @param userPassSendReq User password reset send request.
   * @return User.
   */
  private User resolveUser(UserPassSendReq userPassSendReq) {
    User user = userRepository.findByEmail(userPassSendReq.email())
        .orElseThrow(() -> new UserDoesNotExistException(userPassSendReq.email()));
    if (EnUserStatus.PENDING.equals(user.getStatus()))
      throw new UserMustBeActiveException(userPassSendReq.email());
    if (user.getLocked())
      throw new UserCannotBeLockedException(userPassSendReq.email());
    return user;
  }

  /**
   * Triggers password send event for anyone interested.
   * @param user User data.
   */
  private void triggerPassSendEvent(User user, UserToken token) {
    UserPasswordResetSendEvent userPasswordResetSendEvent = new UserPasswordResetSendEvent(
        user.getId(),
        user.getUsername(),
        user.getEmail(),
        user.getLang(),
        token.getToken(),
        passwordResetTokenExpires
    );
    // Will trigger UserEmailService.sendPasswordResetLink().
    eventPublisher.publishEvent(userPasswordResetSendEvent);
  }

  // //////////////////////////////////////////////////////////////////////////

  /**
   * Actually resets password. It is verified by presence of appropriate token.
   * @param userPassResetReq User password reset request.
   */
  @Transactional
  public void reset(UserPassResetReq userPassResetReq) {
    LocalDateTime nowAt = clockService.getNowUTC();

    UserToken userToken = resolveToken(nowAt, EnTokenType.PASSWORD, userPassResetReq.token());
    User user = userToken.getUser();
    user.setModifiedAt(nowAt);
    user.setPassword(passwordEncoder.encode(userPassResetReq.password()));
    user.getTokens().remove(userToken);
    user.addHistory(createHistoryEvent(nowAt, EnHistoryWhat.PASS_RESET));

    userRepository.save(user);
    triggerPassConfirmEvent(user);
  }

  /**
   * Triggers password send event for anyone interested.
   * @param user User data.
   */
  private void triggerPassConfirmEvent(User user) {
    UserPasswordResetConfirmEvent userPasswordResetConfirmEvent = new UserPasswordResetConfirmEvent(
        user.getId(),
        user.getUsername(),
        user.getEmail(),
        user.getLang()
    );
    // Will trigger UserEmailService.sendPasswordResetConfirmation().
    eventPublisher.publishEvent(userPasswordResetConfirmEvent);
  }
}
