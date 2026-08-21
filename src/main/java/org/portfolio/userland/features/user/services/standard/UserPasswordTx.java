package org.portfolio.userland.features.user.services.standard;

import lombok.RequiredArgsConstructor;
import org.portfolio.userland.features.user.dto.standard.password.UserPassResetLinkReq;
import org.portfolio.userland.features.user.entities.*;
import org.portfolio.userland.features.user.events.UserPasswordResetConfirmEvent;
import org.portfolio.userland.features.user.events.UserPasswordResetRequestEvent;
import org.portfolio.userland.features.user.services.BaseUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Transactional part of user password reset.
 * <p>It is separated from {@link UserPasswordService} so expensive CPU operations (BCrypt hashing/verification) can
 * run outside of transaction and not hold a database connection.</p>
 */
@Service
@RequiredArgsConstructor
@Transactional
public class UserPasswordTx extends BaseUserService {
  /**
   * Creates password reset token and (indirectly, via event) sends email with password reset link. User must be
   * already resolved (see {@link UserPasswordService#send}).
   * @param userPassResetLinkReq User password reset request.
   * @param user Resolved user.
   */
  public void send(UserPassResetLinkReq userPassResetLinkReq, User user) {
    LocalDateTime nowAt = clockService.getNowUTC();
    boolean result = ensureTokenDoesNotExist(nowAt, EnUserTokenType.PASSWORD, user, !build.getTest());
    if (!result) return; // fail silently to prevent email enumeration attack

    // Save token directly via repository.
    UserToken token = createTokenData(nowAt, EnUserTokenType.PASSWORD);
    token.setUser(user);
    userTokenRepository.save(token);

    addHistoryEvent(user, nowAt, EnUserHistoryWho.USER, EnUserHistoryWhat.PASS_RESET_REQ, "");

    triggerPassResetReqEvent(userPassResetLinkReq, user, token);
  }

  /**
   * Actually resets password to given hash. It is verified by presence of appropriate token.
   * @param tokenStr Password reset token string.
   * @param passwordHash Already computed BCrypt hash of the new password.
   */
  public void reset(String tokenStr, String passwordHash) {
    LocalDateTime nowAt = clockService.getNowUTC();

    UserToken userToken = resolveToken(nowAt, EnUserTokenType.PASSWORD, tokenStr);
    User user = userToken.getUser();
    userHelperService.verifyUser(user, false); // must have valid state

    user.setPassword(passwordHash);
    userRepository.save(user); // modifiedAt is maintained automatically by JPA auditing

    addHistoryEvent(user, nowAt, EnUserHistoryWho.USER, EnUserHistoryWhat.PASS_RESET, "");

    triggerPassResetConfirmEvent(user);
  }

  /**
   * Triggers password reset link event for anyone interested.
   * @param userPassResetLinkReq User password reset request.
   * @param user User data.
   * @param token User token data.
   */
  private void triggerPassResetReqEvent(UserPassResetLinkReq userPassResetLinkReq, User user, UserToken token) {
    UserPasswordResetRequestEvent userPasswordResetRequestEvent = new UserPasswordResetRequestEvent(
        user.getId(),
        user.getUsername(),
        user.getEmail(),
        user.getLang(),
        userPassResetLinkReq.frontend(),
        token.getToken(),
        userHelperService.resolveExpirationTime(EnUserTokenType.PASSWORD)
    );
    // Will trigger UserSendEmailService.sendPasswordResetRequest().
    eventPublisher.publishEvent(userPasswordResetRequestEvent);
  }

  /**
   * Triggers password reset confirmation event for anyone interested.
   * @param user User data.
   */
  private void triggerPassResetConfirmEvent(User user) {
    UserPasswordResetConfirmEvent userPasswordResetConfirmEvent = new UserPasswordResetConfirmEvent(
        user.getId(),
        user.getUsername(),
        user.getEmail(),
        user.getLang()
    );
    // Will trigger UserSendEmailService.sendPasswordResetConfirm().
    eventPublisher.publishEvent(userPasswordResetConfirmEvent);
  }
}
