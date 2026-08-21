package org.portfolio.userland.features.user.services.standard;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.portfolio.userland.features.user.dto.standard.register.TokenActivateReq;
import org.portfolio.userland.features.user.dto.standard.register.UserRegisterReq;
import org.portfolio.userland.features.user.services.BaseUserService;
import org.portfolio.userland.system.config.service.ConfigConst;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Entry point for user registration and activation.
 * <p>User story:</p>
 * <ul>
 *   <li>User goes on user registration page and fills form.</li>
 *   <li>User clicks on registration button. Frontend calls <code>/api/users/register</code> endpoint.</li>
 *   <li>System creates pending user account, activation token and sends email with activation link. Note that link leads to frontend.</li>
 *   <li>User clicks on activation link, get redirected to frontend, frontend calls <code>/api/users/activate</code>.</li>
 *   <li>Frontend reacts appropriately to response from <code>/api/users/activate</code> endpoint (show success or failure message).</li>
 *   <li>On successful activation, backend sends email confirming successful user account activation.</li>
 * </ul>
 * <p>Note we do not do anything beyond registration/activation itself here. We trigger events - other services (like
 * user email service sending registration email) will react to it.</p>
 * <p>Note: this class is intentionally NOT transactional. BCrypt password hashing is CPU-heavy; running it outside of
 * transaction prevents holding a database connection for its duration. All database work is done transactionally by
 * {@link UserRegisterTx}.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserRegisterService extends BaseUserService {
  private final PasswordEncoder passwordEncoder;
  private final UserRegisterTx userRegisterTx;

  /**
   * Registers user in system.
   * @param userRegisterReq User registration request.
   */
  public void register(UserRegisterReq userRegisterReq) {
    // Hash password BEFORE entering transaction. BCrypt is CPU-heavy and must not hold a database connection open.
    String passwordHash = passwordEncoder.encode(userRegisterReq.password());
    userRegisterReq = modifyRegistrationReq(userRegisterReq);
    userRegisterTx.register(userRegisterReq, passwordHash);
  }

  /**
   * Modify registration request.
   * @param userRegisterReq User registration request.
   * @return Modified user registration request.
   */
  private UserRegisterReq modifyRegistrationReq(UserRegisterReq userRegisterReq) {
    boolean activate = userRegisterReq.activate() != null && userRegisterReq.activate();
    // Never allow user activation on spot during registration on PROD. Activate field is convenience option for testing during development.
    if (!build.getTest()) activate = false;

    // Never allow admin permissions outside of portfolio mode.
    boolean isAdmin = userRegisterReq.isAdmin() != null && userRegisterReq.isAdmin();
    if (isAdmin) {
      String generalPortfolio = configService.get(ConfigConst.GENERAL_PORTFOLIO, "0");
      if (!ConfigConst.TRUE.equals(generalPortfolio)) isAdmin = false;
    }
    return userRegisterReq.toBuilder()
        .activate(activate)
        .isAdmin(isAdmin)
        .build();
  }

  /**
   * Activate user that has token with given token string.
   * @param tokenActivateReq Token activation request.
   */
  public void activate(TokenActivateReq tokenActivateReq) {
    userRegisterTx.activate(tokenActivateReq);
  }
}
