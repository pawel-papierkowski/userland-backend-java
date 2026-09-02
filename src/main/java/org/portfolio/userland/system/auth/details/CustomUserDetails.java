package org.portfolio.userland.system.auth.details;

import lombok.Getter;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.portfolio.userland.features.user.entities.EnUserStatus;
import org.portfolio.userland.features.user.entities.User;
import org.portfolio.userland.system.auth.perm.PermissionHelper;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * User details specific to custom user. Represents data encoded in JWT and from User entity in database.
 * Usage example 1:
 * <pre>
 * &#064;RestController
 * &#064;RequestMapping("/api")
 * public class ProfileController {
 *   &#064;GetMapping("/me")
 *   public String getMyProfile(@AuthenticationPrincipal CustomUserDetails customUserDetails) {
 *     if (customUserDetails == null) return; // not logged in
 *     // You now have safe, typed access to the authenticated user's details.
 *     Long userId = customUserDetails.getId();
 *     String username = customUserDetails.getUsername();
 *     return "Hello " + username + ", your ID is " + userId;
 *   }
 * }</pre>
 * Usage example 2:
 * <pre>
 * CustomUserDetails customUserDetails = AuthHelper.resolveUserDetails();
 * if (customUserDetails == null) return; // not logged in
 * // do something...
 * </pre>
 */
public class CustomUserDetails implements UserDetails {
  @Getter
  private final Long id;
  @Getter
  private final boolean active;
  @Getter
  private final boolean locked;
  private final String username;
  @Getter
  private final String email;

  private final Collection<? extends GrantedAuthority> authorities;

  /** Contains authority strings. */
  private final Set<String> auths;

  /**
   * Constructor.
   * @param user User data.
   */
  public CustomUserDetails(User user) {
    this.id = user.getId();
    this.active = EnUserStatus.ACTIVE.equals(user.getStatus());
    this.locked = user.getLocked();
    this.username = user.getUsername();
    this.email = user.getEmail();

    this.authorities = PermissionHelper.resolveAuthorities(user.getPermissions());
    this.auths = resolveAuths();
  }

  /**
   * Constructor used by <code>JwtAuthFilter</code> to build details from signed JWT claims combined with user state
   * loaded from database. Permissions are intentionally taken from token claims, not database.
   * Also used by tests for mock purposes.
   * @param id User identificator.
   * @param active Is this user active?
   * @param locked Is this user locked?
   * @param username Username.
   * @param email Email.
   * @param authorities Authorities.
   */
  public CustomUserDetails(Long id, boolean active, boolean locked, String username, String email,
                           Collection<? extends GrantedAuthority> authorities) {
    this.id = id;
    this.active = active;
    this.locked = locked;
    this.username = username;
    this.email = email;
    this.authorities = authorities == null ? List.of() : authorities;
    this.auths = resolveAuths();
  }

  /**
   * Resolve auths from authorities.
   * @return Auths.
   */
  private Set<String> resolveAuths() {
    Set<String> auths = new HashSet<>();
    for (GrantedAuthority grantedAuthority : authorities) {
      auths.add(grantedAuthority.getAuthority());
    }
    return auths;
  }

  //

  @Override
  public boolean isEnabled() {
    return active;
  }

  @Override
  public boolean isAccountNonLocked() {
    return !locked;
  }

  @Override
  public @NonNull String getUsername() {
    return username;
  }

  @Override
  public @Nullable String getPassword() {
    // Password is unused, but method must be present due to UserDetails interface.
    return null;
  }

  @Override
  public @NonNull Collection<? extends GrantedAuthority> getAuthorities() {
    return authorities;
  }

  //

  /**
   * Check if user detail has at least one authority from given list.
   * @param authorities Many authorities.
   * @return True if user detail has at least one from given authorities, otherwise false.
   */
  public boolean hasAnyAuthority(String... authorities) {
    for (String authority : authorities) {
      if (hasAuthority(authority)) return true;
    }
    return false;
  }

  /**
   * Check if user detail has given authority.
   * @param authority Authority to check.
   * @return True if user detail has authority, otherwise false.
   */
  public boolean hasAuthority(String authority) {
    return auths.contains(authority);
  }
}
