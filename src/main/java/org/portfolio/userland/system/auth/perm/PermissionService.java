package org.portfolio.userland.system.auth.perm;

import com.google.api.client.util.Lists;
import lombok.RequiredArgsConstructor;
import org.portfolio.userland.features.user.constants.UserPermConst;
import org.portfolio.userland.features.user.entities.UserPermission;
import org.portfolio.userland.system.auth.AuthHelper;
import org.portfolio.userland.system.auth.details.CustomUserDetails;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Answers questions like "can user do this"? Operates on logged-in user data.
 */
@Service
@RequiredArgsConstructor
public class PermissionService {
  /**
   * Checks if logged-in user has correct permissions specified by permission kind.
   * @return True if given user has correct permissions, otherwise false.
   */
  public boolean has(EnPermKind permKind) {
    return has(permKind, AuthHelper.resolveUserDetails());
  }

  /**
   * Checks if given custom user details has correct permissions specified by permission kind.
   * @param customUserDetails Custom user details.
   * @return True if given user has correct permissions, otherwise false.
   */
  public boolean has(EnPermKind permKind, CustomUserDetails customUserDetails) {
    if (customUserDetails == null) return false; // not logged in means no access
    Map<String, Set<String>> rawPermissions = get(permKind);
    return customUserDetails.hasAnyAuthority(mapToArray(rawPermissions));
  }

  /**
   * Checks if given user permissions are compatible with given permission kind.
   * @param permKind Permission kind.
   * @param userPermissions User permissions.
   * @return True if given user has access to admin panel, otherwise false.
   */
  public boolean has(EnPermKind permKind, Set<UserPermission>  userPermissions) {
    if (userPermissions == null || userPermissions.isEmpty()) return false;
    Map<String, Set<String>> rawPermissions = get(permKind);

    for (UserPermission userPermission : userPermissions) {
      String name = userPermission.getPermission().getName();
      String value = userPermission.getValue();
      if (hasPermission(rawPermissions, name, value)) return true;
    }
    return false;
  }

  //

  /**
   * Check if you have one particular permission.
   * @param permissionMap Map of permissions.
   * @param name Permission name.
   * @param value Permission value.
   * @return True if permission with given name and value is present in permissionMap, otherwise false.
   */
  private boolean hasPermission(Map<String, Set<String>> permissionMap, String name, String value) {
    Set<String> values = permissionMap.get(name);
    if (values == null) return false;
    return values.contains(value);
  }

  /**
   * Convert map of permissions to array of strings representing these permissions. Example:
   * <pre>Map.of("role", Set.of("operator", "admin"))</pre>
   * will be converted to
   * <pre>"ROLE_OPERATOR", "ROLE_ADMIN"</pre>
   * @param permissionsMap Permission map.
   * @return Array of strings that represent permissions.
   */
  private String[] mapToArray(Map<String, Set<String>> permissionsMap) {
    List<String> permissionsList = Lists.newArrayList();
    for (Map.Entry<String, Set<String>> entry : permissionsMap.entrySet()) {
      Set<String> values = entry.getValue();
      for (String permValue : values) {
        permissionsList.add(entry.getKey().toUpperCase() + "_" + permValue.toUpperCase());
      }
    }
    return permissionsList.toArray(new String[] {});
  }

  //

  /**
   * Get permission map for given permission kind.
   * @param permKind Permission kind.
   * @return Map of permissions.
   */
  public Map<String, Set<String>> get(EnPermKind permKind) {
    if (permKind == null) return Map.of();
    return switch (permKind) {
      case ACCESS_TO_ADMIN_PANEL -> Map.of(PermConst.ROLE, Set.of(PermConst.ROLE_ADMIN, PermConst.ROLE_OPERATOR));
      case USER_VIEW -> Map.of(PermConst.ROLE, Set.of(PermConst.ROLE_ADMIN), UserPermConst.USER, Set.of(UserPermConst.USER_VIEW));
      case USER_EDIT -> Map.of(PermConst.ROLE, Set.of(PermConst.ROLE_ADMIN), UserPermConst.USER, Set.of(UserPermConst.USER_EDIT));
    };
  }
}
