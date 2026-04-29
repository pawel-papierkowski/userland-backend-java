package org.portfolio.userland.system.auth.details;

import com.google.common.collect.Sets;
import lombok.Getter;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.portfolio.userland.features.user.entities.EnUserStatus;
import org.portfolio.userland.features.user.entities.User;
import org.portfolio.userland.features.user.entities.UserJwt;
import org.portfolio.userland.features.user.entities.UserPermission;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.*;

/**
 * User details specific to custom user. Represents data encoded in JWT and from User entity in database.
 * Usage example 1:
 * <pre>
 * &#064;RestController
 * &#064;RequestMapping("/api")
 * public class ProfileController {
 *   &#064;GetMapping("/me")
 *   public String getMyProfile(@AuthenticationPrincipal CustomUserDetails customUserDetails) {
 *     // You now have safe, typed access to the authenticated user's details.
 *     Long userId = customUserDetails.getId();
 *     String username = customUserDetails.getUsername();
 *     return "Hello " + username + ", your ID is " + userId;
 *   }
 * }</pre>
 * Usage example 2:
 * <pre>
 * CustomUserDetails customUserDetails = AuthHelper.resolveUserDetails();
 * </pre>
 */
public class CustomUserDetails implements UserDetails {
  @Getter
  private final Long id;
  @Getter
  private final Boolean active;
  @Getter
  private final Boolean locked;
  private final String username;
  @Getter
  private final String email;
  private final String password;

  @Getter
  private final Set<String> jwts;
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
    this.password = user.getPassword(); // Used for login validation, then erased.

    this.authorities = resolveAuthorities(user.getPermissions());
    this.jwts = resolveJwts(user.getJwts());
    this.auths = resolveAuths();
  }

  /**
   * Constructor used only by tests for mock purposes.
   * @param id User identificator.
   * @param active Is this user active?
   * @param locked Is this user locked?
   * @param username Username.
   * @param email Email.
   * @param password Password.
   * @param authorities Authorities.
   */
  public CustomUserDetails(Long id, Boolean active, Boolean locked, String username, String email, String password,
                           Set<String> jwts, Collection<? extends GrantedAuthority> authorities) {
    this.id = id;
    this.active = active;
    this.locked = locked;
    this.username = username;
    this.email = email;
    this.password = password; // Used for login validation, then erased.

    this.jwts = jwts == null ? Set.of() : jwts;
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

  /**
   * Map user permissions to authorities.
   * @param userPermissions User permissions.
   * @return Spring Authorities.
   */
  private Collection<? extends GrantedAuthority> resolveAuthorities(Set<UserPermission> userPermissions) {
    // Map permissions to authorities.
    // Example: permission "role" and userPermission "operator" will result in "ROLE_OPERATOR".
    return userPermissions.stream()
        .filter(userPermission -> userPermission.getPermission().getInAuthorities())
        .map(userPermission -> {
          String authorityStr = userPermission.getPermission().getName().toUpperCase()
              + "_" + userPermission.getValue().toUpperCase();
          return new SimpleGrantedAuthority(authorityStr);
        })
        .sorted(Comparator.comparing(GrantedAuthority::getAuthority)) // sorted by natural key required by UserDetail
        .toList();
  }

  /**
   * Find out all JWTs.
   * @param jwts JWT.
   * @return Set of tokens.
   */
  private Set<String> resolveJwts(Set<UserJwt> jwts) {
    Set<String> tokens = Sets.newHashSet();
    for (UserJwt jwt : jwts) {
      tokens.add(jwt.getToken());
    }
    return tokens;
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
    return password;
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
   * @param authority Authorities.
   * @return True if user detail has authority, otherwise false.
   */
  public boolean hasAuthority(String authority) {
    return auths.contains(authority);
  }
}
