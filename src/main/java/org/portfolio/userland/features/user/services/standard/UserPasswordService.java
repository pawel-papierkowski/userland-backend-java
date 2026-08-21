package org.portfolio.userland.features.user.services.standard;

import lombok.RequiredArgsConstructor;
import org.portfolio.userland.features.user.dto.standard.password.UserPassResetConfirmReq;
import org.portfolio.userland.features.user.dto.standard.password.UserPassResetLinkReq;
import org.portfolio.userland.features.user.entities.User;
import org.portfolio.userland.features.user.services.BaseUserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Entry point for user password reset.
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
 * <p>Note: this class is intentionally NOT transactional. BCrypt hashing/verification is CPU-heavy; running it outside
 * of transaction prevents holding a database connection for its duration. All database work is done transactionally by
 * {@link UserPasswordTx}.</p>
 */
@Service
@RequiredArgsConstructor
public class UserPasswordService extends BaseUserService {
  private final PasswordEncoder passwordEncoder;
  private final UserPasswordTx userPasswordTx;

  /**
   * Creates password reset token and (indirectly, via event) sends email with password reset link to user with given
   * email.
   * @param userPassResetLinkReq User password reset request.
   */
  public void send(UserPassResetLinkReq userPassResetLinkReq) {
    User user = userHelperService.resolveUser(userPassResetLinkReq.email(), !build.getTest());
    if (user == null) return; // fail silently to prevent email enumeration attack on production

    userPasswordTx.send(userPassResetLinkReq, user);
  }

  /**
   * Actually resets password. It is verified by presence of appropriate token.
   * @param userPassResetConfirmReq User password reset confirmation request.
   */
  public void reset(UserPassResetConfirmReq userPassResetConfirmReq) {
    // Hash new password (BCrypt) BEFORE entering transaction - it is CPU-heavy and must not hold a database connection.
    String passwordHash = passwordEncoder.encode(userPassResetConfirmReq.password());
    userPasswordTx.reset(userPassResetConfirmReq.token(), passwordHash);
  }
}
