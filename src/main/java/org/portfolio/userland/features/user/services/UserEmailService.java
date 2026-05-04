package org.portfolio.userland.features.user.services;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.portfolio.userland.features.user.dto.email.UserEmailChangeConfirmReq;
import org.portfolio.userland.features.user.dto.email.UserEmailChangeLinkReq;
import org.portfolio.userland.features.user.entities.EnUserHistoryWhat;
import org.portfolio.userland.features.user.entities.EnUserTokenType;
import org.portfolio.userland.features.user.entities.User;
import org.portfolio.userland.features.user.entities.UserToken;
import org.portfolio.userland.features.user.events.UserEmailChangeConfirmEvent;
import org.portfolio.userland.features.user.events.UserEmailChangeFailEvent;
import org.portfolio.userland.features.user.events.UserEmailChangeRequestEvent;
import org.portfolio.userland.features.user.exceptions.UserEmailAlreadyExistsException;
import org.portfolio.userland.features.user.exceptions.UserWrongPasswordException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Business logic for changing email. Since it is highly sensitive operation (email acts as the user's login and
 * recovery method), process is somewhat involved:
 * <ul>
 *   <li>On frontend user muse be logged. Option to change email should be on profile edit page or similar.</li>
 *   <li>Request: in payload we require both new email address and current password.</li>
 *   <li>Backend verifies password and if new email is already present. In both cases returns same error to prevent email enumeration attack.</li>
 *   <li>Backend creates token and sends TWO emails: warning for old account and email change confirmation link to the new account.</li>
 *   <li>Link leads to special page on frontend where user can click on button. It calls email change confirmation endpoint on backend.</li>
 *   <li>Backend ensures new email was not created in meantime, updates email of user, deletes token and sends email that confirms email change.</li>
 *   <li>Frontend shows result (success or failure).</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class UserEmailService extends BaseUserService {
  /**
   * Creates email change token and (indirectly, via event) sends emails with warning and email change link to user with
   * given email.
   * Note: if email is already taken or password is wrong, will return same error (bad password).
   * @param userEmailChangeLinkReq User email change request.
   */
  @Transactional
  public void send(@Valid UserEmailChangeLinkReq userEmailChangeLinkReq) {
    User user = userHelperService.resolveUser(true);
    if (user == null) throw new UserWrongPasswordException();
    if (user.getEmail().equals(userEmailChangeLinkReq.newEmail())) throw new UserEmailAlreadyExistsException(user.getEmail());
    userHelperService.verifyPassword(user, userEmailChangeLinkReq.password());

    if (userRepository.existsByEmail(userEmailChangeLinkReq.newEmail())) {
      // send two emails: warning for old account and warning for new, existing email
      triggerEmailChangeFailEvent(userEmailChangeLinkReq, user);
      return; // pretend everything is fine, preventing email enumeration attack
    }

    LocalDateTime nowAt = clockService.getNowUTC();
    String params = "old: '"+user.getEmail()+"', new: '"+userEmailChangeLinkReq.newEmail()+"'";
    ensureTokenDoesNotExist(nowAt, EnUserTokenType.EMAIL, user);

    UserToken token = createTokenData(nowAt, EnUserTokenType.EMAIL, userEmailChangeLinkReq.newEmail());
    user.addToken(token);
    user = userRepository.save(user);

    addHistoryEvent(user, nowAt, EnUserHistoryWhat.EMAIL_CHANGE_REQ, params);

    triggerEmailChangeReqEvent(userEmailChangeLinkReq, user, token);
  }

  /**
   * Triggers email change request event for anyone interested.
   * @param userEmailChangeLinkReq User email change request.
   * @param user User data.
   * @param token User token data.
   */
  private void triggerEmailChangeReqEvent(UserEmailChangeLinkReq userEmailChangeLinkReq, User user, UserToken token) {
    UserEmailChangeRequestEvent userEmailChangeRequestEvent = new UserEmailChangeRequestEvent(
        user.getId(),
        user.getUsername(),
        user.getEmail(),
        user.getLang(),
        userEmailChangeLinkReq.frontend(),
        userEmailChangeLinkReq.newEmail(),
        token.getToken(),
        userHelperService.resolveExpirationTime(EnUserTokenType.EMAIL)
    );
    // Will trigger UserSendEmailService.sendEmailChangeRequest().
    eventPublisher.publishEvent(userEmailChangeRequestEvent);
  }

  /**
   * Triggers email change fail event for anyone interested.
   * @param userEmailChangeLinkReq User email change request.
   * @param user User data.
   */
  private void triggerEmailChangeFailEvent(UserEmailChangeLinkReq userEmailChangeLinkReq, User user) {
    UserEmailChangeFailEvent userEmailChangeFailEvent = new UserEmailChangeFailEvent(
        user.getId(),
        user.getUsername(),
        user.getEmail(),
        user.getLang(),
        userEmailChangeLinkReq.frontend(),
        userEmailChangeLinkReq.newEmail()
    );
    // Will trigger UserSendEmailService.sendEmailChangeFail().
    eventPublisher.publishEvent(userEmailChangeFailEvent);
  }

  // //////////////////////////////////////////////////////////////////////////

  /**
   * Actually changes email. It is verified by presence of appropriate token.
   * @param userEmailChangeConfirmReq User email change confirmation request.
   */
  @Transactional
  public void confirm(@Valid UserEmailChangeConfirmReq userEmailChangeConfirmReq) {
    LocalDateTime nowAt = clockService.getNowUTC();

    UserToken userToken = resolveToken(nowAt, EnUserTokenType.EMAIL, userEmailChangeConfirmReq.token());
    if (userRepository.existsByEmail(userToken.getPayload())) throw new UserEmailAlreadyExistsException(userToken.getPayload());

    User user = userToken.getUser();
    userHelperService.verifyUser(user, false); // must have valid state
    String params = "old: '"+user.getEmail()+"', new: '"+userToken.getPayload()+"'";

    user.setModifiedAt(nowAt);
    user.setEmail(userToken.getPayload());
    userRepository.save(user);

    userTokenRepository.deleteToken(userToken.getToken());
    addHistoryEvent(user, nowAt, EnUserHistoryWhat.EMAIL_CHANGE, params);
    
    triggerEmailChangeConfirmEvent(user);
  }

  /**
   * Triggers email change confirmation event for anyone interested.
   * @param user User data.
   */
  private void triggerEmailChangeConfirmEvent(User user) {
    UserEmailChangeConfirmEvent userEmailChangeConfirmEvent = new UserEmailChangeConfirmEvent(
        user.getId(),
        user.getUsername(),
        user.getEmail(),
        user.getLang()
    );
    // Will trigger UserSendEmailService.sendEmailChangeConfirm().
    eventPublisher.publishEvent(userEmailChangeConfirmEvent);
  }
}
