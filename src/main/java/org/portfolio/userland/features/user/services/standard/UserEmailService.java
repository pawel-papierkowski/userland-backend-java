package org.portfolio.userland.features.user.services.standard;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.portfolio.userland.features.user.dto.standard.email.UserEmailChangeConfirmReq;
import org.portfolio.userland.features.user.dto.standard.email.UserEmailChangeLinkReq;
import org.portfolio.userland.features.user.entities.*;
import org.portfolio.userland.features.user.events.UserEmailChangeConfirmEvent;
import org.portfolio.userland.features.user.exceptions.UserEmailAlreadyExistsException;
import org.portfolio.userland.features.user.services.BaseUserService;
import org.springframework.dao.DataIntegrityViolationException;
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
  private final UserEmailTx userEmailTx;

  /**
   * Creates email change token and (indirectly, via event) sends emails with warning and email change link to user.
   * <p>Note 1: this method is intentionally NOT transactional. BCrypt password verification is CPU-heavy; running it
   * outside of transaction prevents holding a database connection for its duration. All database work is done
   * transactionally by {@link UserEmailTx}.</p>
   * <p>Note 2: on production, if email is already taken (or some other problem occurred), will return same error as bad password.</p>
   * @param userEmailChangeLinkReq User email change request.
   */
  public void send(@Valid UserEmailChangeLinkReq userEmailChangeLinkReq) {
    User user = userHelperService.resolveUser(false);

    // Verify password (BCrypt) BEFORE entering transaction - it is CPU-heavy and must not hold a database connection.
    userHelperService.verifyPassword(user, userEmailChangeLinkReq.password());
    userEmailTx.send(userEmailChangeLinkReq, user);
  }

  // //////////////////////////////////////////////////////////////////////////

  /**
   * Actually changes email. It is verified by presence of appropriate token.
   * <p>Note: the real guard against duplicate emails is the unique constraint on <code>users.email</code>; the
   * {@code existsByEmail} check above is only a fast path. If we lose a race against a concurrent email change (or
   * registration) for the same address, the constraint violation is translated into a clean
   * {@link UserEmailAlreadyExistsException}. The whole transaction rolls back in that case - including the token
   * consumption done by <code>resolveToken()</code> - so the token remains valid and can be used for another
   * attempt.</p>
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

    user.setEmail(userToken.getPayload());
    try {
      userRepository.save(user);
      // Flush explicitly - otherwise violation of the unique email constraint would surface at commit time,
      // after this method returned, and could not be translated into a meaningful exception anymore.
      userRepository.flush();
    } catch (DataIntegrityViolationException ex) {
      // We lost the race: another transaction took this email in the meantime. Abort transaction immediately
      // (persistence context is inconsistent) - rollback discards token consumption (so the token stays usable),
      // JWT deletion, history and confirm email.
      throw new UserEmailAlreadyExistsException(userToken.getPayload());
    }

    userJwtRepository.deleteAllByUser(user.getId());
    addHistoryEvent(user, nowAt, EnUserHistoryWho.USER, EnUserHistoryWhat.EMAIL_CHANGE, params);

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
