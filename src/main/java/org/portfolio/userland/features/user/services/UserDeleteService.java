package org.portfolio.userland.features.user.services;

import lombok.RequiredArgsConstructor;
import org.portfolio.userland.features.user.dto.delete.UserDeleteConfirmReq;
import org.portfolio.userland.features.user.dto.delete.UserDeleteLinkReq;
import org.portfolio.userland.features.user.entities.EnUserHistoryWhat;
import org.portfolio.userland.features.user.entities.EnUserTokenType;
import org.portfolio.userland.features.user.entities.User;
import org.portfolio.userland.features.user.entities.UserToken;
import org.portfolio.userland.features.user.events.UserAccountDeleteConfirmEvent;
import org.portfolio.userland.features.user.events.UserAccountDeleteLinkEvent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Business logic for user account deletion.
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
 */
@Service
@RequiredArgsConstructor
public class UserDeleteService extends BaseUserService {
  /** How long before account deletion token expires in minutes. */
  @Value("${app.user.token.deletion.expires}")
  private long deletionTokenExpires;

  /**
   * Creates account deletion token and (indirectly, via event) sends email with account deletion link to user.
   * @param userDeleteLinkReq User account deletion link request.
   */
  @Transactional
  public void send(UserDeleteLinkReq userDeleteLinkReq) {
    User user = userHelperService.resolveUser(userDeleteLinkReq.email(), true);
    if (user == null) return; // prevent email enumeration attack

    LocalDateTime nowAt = clockService.getNowUTC();
    ensureTokenDoesNotExist(nowAt, EnUserTokenType.DELETE, user);

    UserToken token = createTokenData(nowAt, EnUserTokenType.DELETE);
    user.addToken(token);
    user = userRepository.save(user);

    addHistoryEvent(user, nowAt, EnUserHistoryWhat.DELETE_REQ, "");

    triggerDeleteLinkEvent(userDeleteLinkReq, user, token);
  }

  /**
   * Triggers account deletion link event for anyone interested.
   * @param userDeleteLinkReq User account deletion link request.
   * @param user User data.
   * @param token User token data.
   */
  private void triggerDeleteLinkEvent(UserDeleteLinkReq userDeleteLinkReq, User user, UserToken token) {
    UserAccountDeleteLinkEvent userAccountDeleteLinkEvent = new UserAccountDeleteLinkEvent(
        user.getId(),
        user.getUsername(),
        user.getEmail(),
        user.getLang(),
        userDeleteLinkReq.frontend(),
        token.getToken(),
        deletionTokenExpires
    );
    // Will trigger UserEmailService.sendAccountDeleteLink().
    eventPublisher.publishEvent(userAccountDeleteLinkEvent);
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
    // Will trigger UserEmailService.sendAccountDeleteConfirmation().
    eventPublisher.publishEvent(userAccountDeleteConfirmEvent);
  }
}
