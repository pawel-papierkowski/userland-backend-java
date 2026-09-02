package org.portfolio.userland.system.auth.perm;

import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.portfolio.userland.common.exception.SystemMisconfigurationException;
import org.portfolio.userland.features.user.constants.UserPermConst;
import org.portfolio.userland.features.user.entities.UserPermission;
import org.portfolio.userland.system.auth.AuthHelper;
import org.portfolio.userland.system.auth.details.CustomUserDetails;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
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
   * @param permKind Permission kind.
   * @return True if given user has correct permissions, otherwise false.
   */
  public boolean has(EnPermKind permKind) {
    return has(permKind, AuthHelper.resolveUserDetails());
  }

  /**
   * Checks if given custom user details have correct permissions specified by permission kind.
   * @param permKind Permission kind.
   * @param customUserDetails Custom user details.
   * @return True if given user has correct permissions, otherwise false.
   */
  public boolean has(EnPermKind permKind, CustomUserDetails customUserDetails) {
    if (customUserDetails == null) return false; // not logged in means no access
    Map<String, Set<String>> rawPermissions = getMap(permKind);
    return customUserDetails.hasAnyAuthority(mapToArray(rawPermissions));
  }

  /**
   * Checks if given user permissions are compatible with given permission kind.
   * @param permKind Permission kind.
   * @param userPermissions User permissions.
   * @return True if given user has correct permissions, otherwise false.
   */
  public boolean has(EnPermKind permKind, Set<UserPermission>  userPermissions) {
    if (userPermissions == null || userPermissions.isEmpty()) return false;

    if (!Hibernate.isInitialized(userPermissions)) {
      throw new SystemMisconfigurationException(
          "PermissionService.has() called with uninitialized permissions collection. It will cause N+1. "
              + "Use @EntityGraph to eagerly load 'permissions' and 'permissions.permission'.");
    }

    Map<String, Set<String>> rawPermissions = getMap(permKind);

    for (UserPermission userPermission : userPermissions) {
      String name = userPermission.getPermission().getName(); // N+1 if lazy!
      String value = userPermission.getValue();
      if (hasPermission(rawPermissions, name, value)) return true;
    }
    return false;
  }

  //

  /**
   * Check if you have one particular permission. Note: comparison is case-insensitive, so stored data like
   * <code>'ADMIN'</code> matches expected value <code>'admin'</code>. This keeps this overload consistent with
   * authority-based checks, where permission strings are uppercased before comparison.
   * @param permissionMap Map of permissions.
   * @param name Permission name.
   * @param value Permission value.
   * @return True if permission with given name and value is present in permissionMap, otherwise false.
   */
  private boolean hasPermission(Map<String, Set<String>> permissionMap, String name, String value) {
    Set<String> values = null;
    for (Map.Entry<String, Set<String>> entry : permissionMap.entrySet()) {
      if (entry.getKey().equalsIgnoreCase(name)) { // case-insensitive key lookup
        values = entry.getValue();
        break;
      }
    }
    if (values == null) return false;
    for (String candidate : values) {
      if (candidate.equalsIgnoreCase(value)) return true; // case-insensitive value match
    }
    return false; // nothing matched
  }

  /**
   * Convert map of permissions to array of strings representing these permissions. Example:
   * <pre>Map.of("role", Set.of("operator", "admin"))</pre>
   * will be converted to
   * <pre>"ROLE_OPERATOR", "ROLE_ADMIN"</pre>
   * <p>Note: format of authority strings is defined in {@link PermissionHelper#buildAuthority(String, String)}.</p>
   * @param permissionsMap Permission map.
   * @return Array of strings that represent permissions.
   */
  private String[] mapToArray(Map<String, Set<String>> permissionsMap) {
    List<String> permissionsList = new ArrayList<>();
    for (Map.Entry<String, Set<String>> entry : permissionsMap.entrySet()) {
      Set<String> values = entry.getValue();
      for (String permValue : values) {
        permissionsList.add(PermissionHelper.buildAuthority(entry.getKey(), permValue));
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
  public Map<String, Set<String>> getMap(EnPermKind permKind) {
    if (permKind == null) return Map.of();
    return switch (permKind) {
      case ADMIN_ONLY -> Map.of(PermConst.ROLE, Set.of(PermConst.ROLE_ADMIN));
      case ACCESS_TO_ADMIN_PANEL -> Map.of(PermConst.ROLE, Set.of(PermConst.ROLE_ADMIN, PermConst.ROLE_OPERATOR));
      case USER_VIEW -> Map.of(PermConst.ROLE, Set.of(PermConst.ROLE_ADMIN), UserPermConst.USER, Set.of(UserPermConst.USER_VIEW));
      case USER_EDIT -> Map.of(PermConst.ROLE, Set.of(PermConst.ROLE_ADMIN), UserPermConst.USER, Set.of(UserPermConst.USER_EDIT));
    };
  }
}
