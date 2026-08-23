package org.portfolio.userland.system.auth.perm;

import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;
import org.portfolio.userland.features.user.entities.Permission;
import org.portfolio.userland.features.user.entities.User;
import org.portfolio.userland.features.user.entities.UserPermission;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.*;

/**
 * Helper for permissions-related code.
 */
public class PermissionHelper {
  private PermissionHelper() {
  }

  /**
   * Map user permissions to authorities.
   * <p>Example: permission <code>role</code> and userPermission <code>operator</code> will result in <code>ROLE_OPERATOR</code>.</p>
   * @param userPermissions User permissions.
   * @return Spring Authorities.
   */
  public static Collection<? extends GrantedAuthority> resolveAuthorities(Set<UserPermission> userPermissions) {
    return userPermissions.stream()
        .map(userPermission -> {
          String authorityStr = userPermission.getPermission().getName().toUpperCase()
              + "_" + userPermission.getValue().toUpperCase();
          return new SimpleGrantedAuthority(authorityStr);
        })
        .sorted(Comparator.comparing(GrantedAuthority::getAuthority)) // sorted by natural key required by UserDetail
        .toList();
  }

  /**
   * Map permissions from JWT <code>perms</code> claim back to authorities. This is reverse of
   * {@link #resolvePermissions(User)} followed by {@link #resolveAuthorities(Set)}.
   * <p>Example: claim entry <code>"role" -> "admin,operator"</code> will result in <code>ROLE_ADMIN</code> and
   * <code>ROLE_OPERATOR</code>.</p>
   * <p>Note: it is safe to use claims as source of authorities because the JWT is signed - the client cannot modify
   * claims without invalidating the signature.</p>
   * @param permsClaim Permissions as stored in <code>perms</code> claim.
   * @return Spring Authorities.
   */
  public static Collection<? extends GrantedAuthority> resolveAuthoritiesFromClaim(Map<?, ?> permsClaim) {
    if (permsClaim == null) return List.of();

    List<SimpleGrantedAuthority> authorities = new ArrayList<>();
    for (Map.Entry<?, ?> entry : permsClaim.entrySet()) {
      String permissionName = String.valueOf(entry.getKey());
      String permValues = entry.getValue() == null ? "" : entry.getValue().toString();
      for (String permValue : permValues.split(",")) {
        if (StringUtils.isBlank(permValue)) continue;
        authorities.add(new SimpleGrantedAuthority(permissionName.toUpperCase()
            + "_" + permValue.trim().toUpperCase()));
      }
    }

    return authorities.stream()
        .sorted(Comparator.comparing(GrantedAuthority::getAuthority)) // sorted by natural key required by UserDetail
        .toList();
  }

  /**
   * Map user permissions. Key is name of permission, value is permission values separated by comma. Values are sorted.
   * <p>Example: name <code>role</code> and permissions <code>operator</code>, <code>admin</code> will be saved as
   * <code>"role" -> "admin,operator"</code>.</p>
   * @param user User data.
   * @return Permissions as <code>Map</code>.
   */
  public static Map<String, String> resolvePermissions(User user) {
    Map<String, String> claimMap = Maps.newHashMap();

    // We need to have sorted list of permissions to ensure consistent results.
    List<UserPermission> permissions = user.getPermissions()
        .stream()
        .sorted(Comparator.comparing(UserPermission::getValue))
        .toList();

    for (UserPermission permissionEntry : permissions) {
      Permission permission = permissionEntry.getPermission();
      String permValue = "";
      if (claimMap.containsKey(permission.getName())) permValue = claimMap.get(permission.getName());
      if (StringUtils.isNotEmpty(permValue)) permValue += ",";
      permValue += permissionEntry.getValue();
      claimMap.put(permission.getName(), permValue);
    }

    return claimMap;
  }
}
