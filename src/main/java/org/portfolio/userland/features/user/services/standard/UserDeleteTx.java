package org.portfolio.userland.features.user.services.standard;

import lombok.RequiredArgsConstructor;
import org.portfolio.userland.features.user.dto.standard.delete.UserDeleteLinkReq;
import org.portfolio.userland.features.user.entities.*;
import org.portfolio.userland.features.user.events.UserAccountDeleteRequestEvent;
import org.portfolio.userland.features.user.services.BaseUserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Transactional part of account deletion request.
 * <p>It is separated from {@link UserDeleteService} so expensive CPU operations (BCrypt password verification) can run
 * outside of transaction and not hold a database connection.</p>
 */
@Service
@RequiredArgsConstructor
@Transactional
public class UserDeleteTx extends BaseUserService {
  /** How long before account deletion token expires in minutes. */
  @Value("${app.user.token.deletion.expires}")
  private long deletionTokenExpires;

  /**
   * Creates account deletion token and (indirectly, via event) sends email with account deletion link. Password
   * verification must be already done (see {@link UserDeleteService#send}).
   * @param userDeleteLinkReq User account deletion link request.
   * @param user Resolved and verified user.
   */
  public void send(UserDeleteLinkReq userDeleteLinkReq, User user) {
    LocalDateTime nowAt = clockService.getNowUTC();
    ensureTokenDoesNotExist(nowAt, EnUserTokenType.DELETE, user);

    // Save token atomically - guards also against concurrent creation of same token type.
    UserToken token = persistNewToken(user, nowAt, EnUserTokenType.DELETE);

    addHistoryEvent(user, nowAt, EnUserHistoryWho.USER, EnUserHistoryWhat.DELETE_REQ, "");

    triggerDeleteReqEvent(userDeleteLinkReq, user, token);
  }

  /**
   * Triggers account deletion link event for anyone interested.
   * @param userDeleteLinkReq User account deletion link request.
   * @param user User data.
   * @param token User token data.
   */
  private void triggerDeleteReqEvent(UserDeleteLinkReq userDeleteLinkReq, User user, UserToken token) {
    UserAccountDeleteRequestEvent userAccountDeleteRequestEvent = new UserAccountDeleteRequestEvent(
        user.getId(),
        user.getUsername(),
        user.getEmail(),
        user.getLang(),
        userDeleteLinkReq.frontend(),
        token.getToken(),
        deletionTokenExpires
    );
    // Will trigger UserSendEmailService.sendAccountDeleteRequest().
    eventPublisher.publishEvent(userAccountDeleteRequestEvent);
  }
}
