package org.portfolio.userland.system.auth;

import com.google.api.client.util.Lists;
import lombok.RequiredArgsConstructor;
import org.portfolio.userland.features.user.constants.UserPermConst;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

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
   * Checks if given user has access to admin panel.
   * @param customUserDetails Custom user details.
   * @return True if given user has access to admin panel, otherwise false.
   */
  public boolean hasAccessToAdminPanel(CustomUserDetails customUserDetails) {
    if (customUserDetails == null) return false; // not logged in means no access
    return customUserDetails.hasAuthorities(mapToArray(hasAccessToAdminPanelMap()));
  }

  /**
   * Get permissions that have access to administration panel.
   * @return Map of permissions.
   */
  public Map<String, List<String>> hasAccessToAdminPanelMap() {
    return Map.of(UserPermConst.ROLE, List.of(UserPermConst.OPERATOR, UserPermConst.ADMIN));
  }

  //

  /**
   * Convert map of permissions to array of strings representing these permissions. Example:
   * <pre>Map.of("role", List.of("operator", "admin"))</pre>
   * will be converted to
   * <pre>"ROLE_OPERATOR", "ROLE_ADMIN"</pre>
   * @param permissionsMap Permission map.
   * @return Array of strings that represent permissions.
   */
  private String[] mapToArray(Map<String, List<String>> permissionsMap) {
    List<String> permissionsList = Lists.newArrayList();
    for (Map.Entry<String, List<String>> entry : permissionsMap.entrySet()) {
      List<String> values = entry.getValue();
      for (String permValue : values) {
        permissionsList.add(entry.getKey().toUpperCase() + "_" + permValue.toUpperCase());
      }
    }
    return permissionsList.toArray(new String[] {});
  }
}
