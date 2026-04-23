package org.portfolio.userland.system.auth;

import lombok.RequiredArgsConstructor;
import org.portfolio.userland.features.user.constants.UserPermConst;
import org.springframework.stereotype.Service;

/**
 * Answers questions like "can user do this"? Operates on logged-in user data.
 */
@Service
@RequiredArgsConstructor
public class PermissionService {
  /**
   * Checks if logged-in user has access to admin panel.
   * @return True if given user has access to admin panel, otherwise false.
   */
  public boolean hasAccessToAdminPanel() {
    return hasAccessToAdminPanel(AuthHelper.resolveUserDetails());
  }

  /**
   * Checks if logged-in user has access to admin panel.
   * @param customUserDetails Custom user details.
   * @return True if given user has access to admin panel, otherwise false.
   */
  public boolean hasAccessToAdminPanel(CustomUserDetails customUserDetails) {
    if (customUserDetails == null) return false; // not logged in means no access
    return customUserDetails.hasAuthorities(UserPermConst.ROLE_OPERATOR, UserPermConst.ROLE_ADMIN);
  }
}
