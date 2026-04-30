package org.portfolio.userland.features.user.services;

import lombok.RequiredArgsConstructor;
import org.portfolio.userland.common.services.http.HttpHelperService;
import org.portfolio.userland.features.user.dto.login.UserLoginReq;
import org.portfolio.userland.features.user.dto.login.UserLoginResp;
import org.portfolio.userland.features.user.entities.EnUserHistoryWhat;
import org.portfolio.userland.features.user.entities.User;
import org.portfolio.userland.features.user.exceptions.UserWrongPasswordException;
import org.portfolio.userland.system.auth.AuthHelper;
import org.portfolio.userland.system.auth.details.CustomUserDetails;
import org.portfolio.userland.system.auth.jwt.JwtService;
import org.portfolio.userland.system.auth.perm.EnPermKind;
import org.portfolio.userland.system.auth.perm.PermissionService;
import org.portfolio.userland.system.config.service.ConfigConst;
import org.portfolio.userland.system.config.service.ConfigService;
import org.portfolio.userland.system.lockdown.exceptions.UserLockdownException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Handles user login and logout.
 */
@Service
@RequiredArgsConstructor
public class UserLoginService extends BaseUserService {
  private final ConfigService configService;
  private final PermissionService permissionService;
  private final JwtService jwtService;
  private final PasswordEncoder passwordEncoder;
  private final HttpHelperService httpHelperService;

  /**
   * Perform user login.
   * @param userLoginReq User login request.
   * @return User login response.
   */
  @Transactional
  public UserLoginResp login(UserLoginReq userLoginReq) {
    User user = userHelperService.resolveUser(userLoginReq.email());
    verifyPassword(user, userLoginReq.password());
    verifyLockdown(user);

    LocalDateTime nowAt = clockService.getNowUTC();

    // Login is successful. Generate JWT token now.
    String jwtToken = jwtService.generateToken(user);
    // Add JWT in database. This will allow us to effectively revoke tokens later (logout etc).
    addJwtEntry(user, nowAt, jwtToken);
    // Add login event to user history.
    addHistoryEvent(user, nowAt, EnUserHistoryWhat.LOGIN, httpHelperService.resolveHttpParams());
    return new UserLoginResp(jwtToken);
  }

  /**
   * Verifies if password is correct. If it is not, throws exception.
   * @param user User data.
   * @param rawPassword Given password.
   */
  private void verifyPassword(User user, String rawPassword) {
    boolean isMatch = passwordEncoder.matches(rawPassword, user.getPassword());
    if (!isMatch) throw new UserWrongPasswordException(user.getEmail());
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
    if (customUserDetails == null) return;

    // If we are logged in, add entry in history and remove JWT entries in database. It will invalidate any JWT that
    // might be in circulation.
    LocalDateTime nowAt = clockService.getNowUTC();
    User user = userHelperService.resolveUser(customUserDetails.getEmail());
    addHistoryEvent(user, nowAt, EnUserHistoryWhat.LOGOUT, "");
    userJwtRepository.deleteAllByUser(user.getId()); // Revoke all JWTs related to this user.
  }
}
