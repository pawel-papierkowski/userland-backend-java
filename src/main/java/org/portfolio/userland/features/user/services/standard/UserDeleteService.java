package org.portfolio.userland.features.user.services.standard;

import lombok.RequiredArgsConstructor;
import org.portfolio.userland.features.user.dto.standard.delete.UserDeleteConfirmReq;
import org.portfolio.userland.features.user.dto.standard.delete.UserDeleteLinkReq;
import org.portfolio.userland.features.user.entities.EnUserTokenType;
import org.portfolio.userland.features.user.entities.User;
import org.portfolio.userland.features.user.entities.UserToken;
import org.portfolio.userland.features.user.events.UserAccountDeleteConfirmEvent;
import org.portfolio.userland.features.user.services.BaseUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Business logic for account deletion.
 * <p>User story:</p>
 * <ul>
 *   <li>User on frontend clicks "Delete account" option.</li>
 *   <li>Frontend calls <code>/api/users/delete/link</code> endpoint.</li>
 *   <li>System creates account deletion token and sends account deletion email with link to frontend.</li>
 *   <li>User clicks on link and gets redirected to separate page where they can confirm that yes, they really want to delete account.</li>
 *   <li>User clicks on account delete button. Frontend calls <code>/api/users/delete/confirm</code>.</li>
 *   <li>Backend verifies call and in case of success removes user account from database and sends email confirming successful account deletion.</li>
 *   <li>Frontend reacts appropriately to response from account delete endpoint (show success or failure message).</li>
 * </ul>
 * <p>Note: this class is intentionally NOT transactional (where BCrypt verification is involved). BCrypt is CPU-heavy;
 * running it outside of transaction prevents holding a database connection for its duration. All database work is done
 * transactionally by {@link UserDeleteTx}.</p>
 */
@Service
@RequiredArgsConstructor
public class UserDeleteService extends BaseUserService {
  private final UserDeleteTx userDeleteTx;

  /**
   * Creates account deletion token and (indirectly, via event) sends email with account deletion link to user.
   * @param userDeleteLinkReq User account deletion link request.
   */
  public void send(UserDeleteLinkReq userDeleteLinkReq) {
    User user = userHelperService.resolveUser(false);

    // Verify password (BCrypt) BEFORE entering transaction - it is CPU-heavy and must not hold a database connection.
    userHelperService.verifyPassword(user, userDeleteLinkReq.password());
    userDeleteTx.send(userDeleteLinkReq, user);
  }

  // //////////////////////////////////////////////////////////////////////////

  /**
   * Actually deletes the account. It is verified by presence of appropriate token.
   * @param userDeleteConfirmReq User account deletion request.
   */
  @Transactional
  public void delete(UserDeleteConfirmReq userDeleteConfirmReq) {
    LocalDateTime nowAt = clockService.getNowUTC();

    UserToken userToken = resolveToken(nowAt, EnUserTokenType.DELETE, userDeleteConfirmReq.token());
    User user = userToken.getUser();
    userHelperService.verifyUser(user, false); // must have valid state

    // Note this removes user completely from system without trace. In real system this likely will be more complex,
    // for example account is preserved but anonymized because you must preserve invoices and other data
    // required by law.
    userRepository.delete(user);

    triggerDeleteConfirmEvent(user);
  }

  /**
   * Triggers account deletion confirmation event for anyone interested.
   * @param user User data.
   */
  private void triggerDeleteConfirmEvent(User user) {
    UserAccountDeleteConfirmEvent userAccountDeleteConfirmEvent = new UserAccountDeleteConfirmEvent(
        user.getId(),
        user.getUsername(),
        user.getEmail(),
        user.getLang()
    );
    // Will trigger UserSendEmailService.sendAccountDeleteConfirm().
    eventPublisher.publishEvent(userAccountDeleteConfirmEvent);
  }
}
