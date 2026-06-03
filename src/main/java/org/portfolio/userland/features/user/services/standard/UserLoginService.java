package org.portfolio.userland.features.user.services.standard;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.portfolio.userland.common.services.web.HttpHelperService;
import org.portfolio.userland.features.user.constants.UserConfigConst;
import org.portfolio.userland.features.user.dto.standard.login.UserLoginReq;
import org.portfolio.userland.features.user.dto.standard.login.UserLoginResp;
import org.portfolio.userland.features.user.dto.standard.login.UserProlongResp;
import org.portfolio.userland.features.user.entities.EnUserHistoryWhat;
import org.portfolio.userland.features.user.entities.EnUserHistoryWho;
import org.portfolio.userland.features.user.entities.User;
import org.portfolio.userland.features.user.exceptions.UserWrongPasswordException;
import org.portfolio.userland.features.user.services.BaseUserService;
import org.portfolio.userland.system.auth.AuthHelper;
import org.portfolio.userland.system.auth.details.CustomUserDetails;
import org.portfolio.userland.system.auth.jwt.JwtService;
import org.portfolio.userland.system.auth.perm.EnPermKind;
import org.portfolio.userland.system.auth.perm.PermissionService;
import org.portfolio.userland.system.config.service.ConfigConst;
import org.portfolio.userland.system.config.service.ConfigService;
import org.portfolio.userland.system.lockdown.exceptions.UserLockdownException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Handles user login and logout.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserLoginService extends BaseUserService {
  private final UserConfigService userConfigService;

  private final ConfigService configService;
  private final PermissionService permissionService;
  private final JwtService jwtService;
  private final HttpHelperService httpHelperService;

  /**
   * Perform user login.
   * @param userLoginReq User login request.
   * @return User login response.
   */
  @Transactional
  public UserLoginResp login(UserLoginReq userLoginReq) {
    // We want to make sure attacker cannot distinguish case when email does not exist and case when wrong password was
    // given. We just say that was wrong password in both cases.
    User user = userHelperService.resolveAuthUser(userLoginReq.email(), true);
    if (user == null) throw new UserWrongPasswordException(); // prevent email enumeration attack

    userHelperService.verifyPassword(user, userLoginReq.password());
    verifyLockdown(user);

    LocalDateTime nowAt = clockService.getNowUTC();

    // Login is successful. Generate JWT token now.
    String jwtToken = generateJwt(user);
    // Add JWT in database. This will allow us to effectively revoke tokens later (logout etc).
    addJwtEntry(user, nowAt, jwtToken);
    // Add login event to user history.
    addHistoryEvent(user, nowAt, EnUserHistoryWho.USER, EnUserHistoryWhat.LOGIN, httpHelperService.resolveHttpParams());

    log.trace("User '{}' has been logged in.", user.getEmail());
    return new UserLoginResp(jwtToken);
  }

  /**
   * Verify lockdown. Normally lockdown will prevent calling any endpoint used in system in first place, but endpoint
   * <code>/api/users/login</code> is exempt from lockdown, so we need to check for it separately. Exemption exists
   * because we want to allow admin user logging in during lockdown while rejecting other users.
   * @param user User.
   */
  private void verifyLockdown(User user) {
    // Do we have lockdown?
    if (ConfigConst.FALSE.equals(configService.get(ConfigConst.USER_LOCKDOWN, ConfigConst.USER_LOCKDOWN_DEF))) return;
    // So we have lockdown. If user has correct permissions, they are still allowed to log in.
    if (permissionService.has(EnPermKind.ACCESS_TO_ADMIN_PANEL, user.getPermissions())) return;
    // Standard user is trying to log in during lockdown, reject them.
    throw new UserLockdownException(user.getEmail());
  }

  // //////////////////////////////////////////////////////////////////////////

  /**
   * Perform user logout. If there is no login, nothing happens.
   */
  @Transactional
  public void logout() {
    // If we aren't logged in, just end. Nothing to do.
    CustomUserDetails customUserDetails = AuthHelper.resolveUserDetails();
    if (customUserDetails == null) {
      log.trace("Cannot log out, user not found.");
      return;
    }

    // If we are logged in, add entry in history and remove JWT entries in database. It will invalidate any JWT that
    // might be in circulation.
    LocalDateTime nowAt = clockService.getNowUTC();
    User user = userHelperService.resolveUser(customUserDetails.getEmail(), false);
    userJwtRepository.deleteAllByUser(user.getId()); // Revoke all JWTs related to this user.
    addHistoryEvent(user, nowAt, EnUserHistoryWho.USER, EnUserHistoryWhat.LOGOUT, "");

    log.trace("User '{}' has been logged out.", user.getEmail());
  }

  // //////////////////////////////////////////////////////////////////////////

  /**
   * Perform prolongation of user session.
   */
  @Transactional
  public UserProlongResp prolong() {
    // If we are logged in, add entry in history, remove old JWT entry and add new JWT entry.
    LocalDateTime nowAt = clockService.getNowUTC();
    User user = userHelperService.resolveAuthUser(false);
    userJwtRepository.deleteAllByUser(user.getId()); // Revoke all JWTs related to this user.
    String jwtToken = generateJwt(user);
    addJwtEntry(user, nowAt, jwtToken); // Add new JWT.
    addHistoryEvent(user, nowAt, EnUserHistoryWho.USER, EnUserHistoryWhat.PROLONG, "");

    log.trace("Session of user '{}' has been prolonged.", user.getEmail());
    return UserProlongResp.builder()
        .jwtToken(jwtToken)
        .build();
  }

  // //////////////////////////////////////////////////////////////////////////

  /**
   * Generate JWT token.
   * @param user User.
   * @return JWT token.
   */
  private String generateJwt(User user) {
    Long jwtExpire = userConfigService.getLong(user, UserConfigConst.JWT_EXPIRE, null);
    return jwtService.generateToken(user, jwtExpire);
  }
}
