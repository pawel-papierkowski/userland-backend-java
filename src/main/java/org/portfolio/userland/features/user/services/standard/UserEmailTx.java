package org.portfolio.userland.features.user.services.standard;

import lombok.RequiredArgsConstructor;
import org.portfolio.userland.features.user.dto.standard.email.UserEmailChangeLinkReq;
import org.portfolio.userland.features.user.entities.*;
import org.portfolio.userland.features.user.events.UserEmailChangeFailEvent;
import org.portfolio.userland.features.user.events.UserEmailChangeRequestEvent;
import org.portfolio.userland.features.user.exceptions.UserEmailAlreadyExistsException;
import org.portfolio.userland.features.user.services.BaseUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Transactional part of email change request.
 * <p>It is separated from {@link UserEmailService} so expensive CPU operations (BCrypt password verification) can run
 * outside of transaction and not hold a database connection.</p>
 */
@Service
@RequiredArgsConstructor
@Transactional
public class UserEmailTx extends BaseUserService {
  /**
   * Creates email change token and (indirectly, via event) sends emails with warning and email change link. Password
   * verification must be already done (see {@link UserEmailService#send}).
   * <p>Note: on production, if email is already taken (or some other problem occurred), will return same error as bad
   * password.</p>
   * @param userEmailChangeLinkReq User email change request.
   * @param user Resolved and verified user.
   */
  public void send(UserEmailChangeLinkReq userEmailChangeLinkReq, User user) {
    if (user.getEmail().equals(userEmailChangeLinkReq.newEmail())) { // same email given
      throw new UserEmailAlreadyExistsException(user.getEmail());
    }

    if (userRepository.existsByEmail(userEmailChangeLinkReq.newEmail())) {
      // send two emails: warning for old account and warning for existing email
      triggerEmailChangeFailEvent(userEmailChangeLinkReq, user);
      return; // pretend everything is fine, preventing email enumeration attack
    }

    LocalDateTime nowAt = clockService.getNowUTC();
    String params = "old: '"+user.getEmail()+"', new: '"+userEmailChangeLinkReq.newEmail()+"'";
    ensureTokenDoesNotExist(nowAt, EnUserTokenType.EMAIL, user);

    // Save token atomically - guards also against concurrent creation of same token type.
    UserToken token = persistNewToken(user, nowAt, EnUserTokenType.EMAIL, userEmailChangeLinkReq.newEmail());

    addHistoryEvent(user, nowAt, EnUserHistoryWho.USER, EnUserHistoryWhat.EMAIL_CHANGE_REQ, params);

    // send two emails: warning for old account and link for email change in new email
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
}
