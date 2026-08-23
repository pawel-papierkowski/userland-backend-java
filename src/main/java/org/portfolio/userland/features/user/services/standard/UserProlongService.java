package org.portfolio.userland.features.user.services.standard;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.portfolio.userland.common.exception.ShouldNeverHappenException;
import org.portfolio.userland.features.user.constants.UserConfigConst;
import org.portfolio.userland.features.user.dto.standard.login.UserProlongResp;
import org.portfolio.userland.features.user.entities.User;
import org.portfolio.userland.features.user.services.BaseUserService;
import org.portfolio.userland.system.auth.AuthHelper;
import org.portfolio.userland.system.auth.details.CustomUserDetails;
import org.portfolio.userland.system.auth.jwt.JwtService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Handles user session prolongation. It is done via revoking existing JWT and issuing new JWT (like in login).
 * <p>Revocation of all sessions (not just current one) is intentional.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserProlongService extends BaseUserService {
  private final UserConfigService userConfigService;

  private final JwtService jwtService;
  private final UserProlongTx userProlongTx;

  /**
   * Perform prolongation of user session.
   * <p>Note: this method is intentionally NOT transactional. JWT generation is a CPU operation; running it outside
   * of transaction prevents holding a database connection for its duration. Only the resulting writes are done
   * transactionally by {@link UserProlongTx#saveProlong}.</p>
   * @return User prolong response.
   */
  public UserProlongResp prolong() {
    // If we are not logged in, we cannot prolong anything. Endpoint requires auth, so this is just a safety net.
    CustomUserDetails customUserDetails = AuthHelper.resolveUserDetails();
    if (customUserDetails == null) {
      log.trace("Cannot prolong session, user not found.");
      throw new ShouldNeverHappenException("User details should exist!");
    }

    // Load user with fresh permissions (needed to rebuild perms claim in new token) and resolve config.
    LocalDateTime nowAt = clockService.getNowUTC();
    User user = userHelperService.resolveAuthUser(customUserDetails.getId(), false);
    Long jwtExpire = userConfigService.getLong(user, UserConfigConst.JWT_EXPIRE, null);

    // Generate JWT token (outside of transaction) and then persist it with history event.
    String jwtToken = jwtService.generateToken(user, jwtExpire);
    userProlongTx.saveProlong(user.getId(), nowAt, jwtToken, jwtExpire);

    log.trace("Session of user '{}' has been prolonged.", user.getEmail());
    return UserProlongResp.builder()
        .jwtToken(jwtToken)
        .build();
  }
}
