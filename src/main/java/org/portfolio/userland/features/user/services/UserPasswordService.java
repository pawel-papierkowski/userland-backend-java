package org.portfolio.userland.features.user.services;

import lombok.RequiredArgsConstructor;
import org.portfolio.userland.features.user.dto.password.UserPassResetConfirmReq;
import org.portfolio.userland.features.user.dto.password.UserPassResetLinkReq;
import org.portfolio.userland.features.user.entity.EnHistoryWhat;
import org.portfolio.userland.features.user.entity.EnTokenType;
import org.portfolio.userland.features.user.entity.User;
import org.portfolio.userland.features.user.entity.UserToken;
import org.portfolio.userland.features.user.events.UserPasswordResetConfirmEvent;
import org.portfolio.userland.features.user.events.UserPasswordResetLinkEvent;
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
 *   <li>User fills form (email) and clicks on button. Frontend calls <code>/api/users/password/link</code> endpoint.</li>
 *   <li>System creates password reset token and sends password reset email with link to frontend.</li>
 *   <li>User clicks on link and gets redirected to separate page where they can enter new password.</li>
 *   <li>User clicks on reset password button. Frontend calls <code>/api/users/password/confirm</code>.</li>
 *   <li>Backend verifies call and in case of success changes password to new one and sends email confirming successful password reset.</li>
 *   <li>Frontend reacts appropriately to response from password reset endpoint (show success or failure message).</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class UserPasswordService extends BaseUserService {
  private final PasswordEncoder passwordEncoder;

  /** How long before password reset token expires in minutes. */
  @Value("${app.user.token.password.expires}")
  private long passwordResetTokenExpires;

  /**
   * Creates password reset token and (indirectly, via event) sends email with password reset link to user with given
   * email.
   * @param userPassResetLinkReq User password reset link request.
   */
  @Transactional
  public void send(UserPassResetLinkReq userPassResetLinkReq) {
    User user = resolveUser(userPassResetLinkReq.email());

    LocalDateTime nowAt = clockService.getNowUTC();
    verifyTokenDoesNotExist(nowAt, EnTokenType.PASSWORD, user);

    UserToken token = createTokenData(nowAt, EnTokenType.PASSWORD);
    user.addToken(token);
    user.addHistory(createHistoryEvent(nowAt, EnHistoryWhat.PASS_RESET_REQ));
    user = userRepository.save(user);

    triggerPassLinkEvent(userPassResetLinkReq, user, token);
  }

  /**
   * Triggers password reset link event for anyone interested.
   * @param userPassResetLinkReq User password reset link request.
   * @param user User data.
   * @param token User token data.
   */
  private void triggerPassLinkEvent(UserPassResetLinkReq userPassResetLinkReq, User user, UserToken token) {
    UserPasswordResetLinkEvent userPasswordResetLinkEvent = new UserPasswordResetLinkEvent(
        user.getId(),
        user.getUsername(),
        user.getEmail(),
        user.getLang(),
        userPassResetLinkReq.frontend(),
        token.getToken(),
        passwordResetTokenExpires
    );
    // Will trigger UserEmailService.sendPasswordResetLink().
    eventPublisher.publishEvent(userPasswordResetLinkEvent);
  }

  // //////////////////////////////////////////////////////////////////////////

  /**
   * Actually resets password. It is verified by presence of appropriate token.
   * @param userPassResetConfirmReq User password reset request.
   */
  @Transactional
  public void reset(UserPassResetConfirmReq userPassResetConfirmReq) {
    LocalDateTime nowAt = clockService.getNowUTC();

    UserToken userToken = resolveToken(nowAt, EnTokenType.PASSWORD, userPassResetConfirmReq.token());
    User user = userToken.getUser();
    user.setModifiedAt(nowAt);
    user.setPassword(passwordEncoder.encode(userPassResetConfirmReq.password()));
    user.getTokens().remove(userToken);
    user.addHistory(createHistoryEvent(nowAt, EnHistoryWhat.PASS_RESET));

    userRepository.save(user);
    triggerPassConfirmEvent(user);
  }

  /**
   * Triggers password reset confirmation event for anyone interested.
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
