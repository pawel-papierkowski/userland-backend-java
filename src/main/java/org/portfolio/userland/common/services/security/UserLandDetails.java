package org.portfolio.userland.common.services.security;

import com.google.common.collect.Sets;
import lombok.Getter;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.portfolio.userland.features.user.entities.EnUserStatus;
import org.portfolio.userland.features.user.entities.User;
import org.portfolio.userland.features.user.entities.UserJwt;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * User details specific to UserLand.
 * Usage example:
 * <pre>
 * &#064;RestController
 * &#064;RequestMapping("/api")
 * public class ProfileController {
 *   &#064;GetMapping("/me")
 *   public String getMyProfile(@AuthenticationPrincipal UserLandDetails principal) {
 *     // You now have safe, typed access to the authenticated user's details.
 *     Long userId = principal.getId();
 *     String username = principal.getUsername();
 *     return "Hello " + username + ", your ID is " + userId;
 *   }
 * }</pre>
 */
public class UserLandDetails implements UserDetails {
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

  /**
   * Constructor.
   * @param user User data.
   * @param authorities Authorities.
   */
  public UserLandDetails(User user, Collection<? extends GrantedAuthority> authorities) {
    this.id = user.getId();
    this.active = EnUserStatus.ACTIVE.equals(user.getStatus());
    this.locked = user.getLocked();
    this.username = user.getUsername();
    this.email = user.getEmail();
    this.password = user.getPassword(); // Used for login validation, then erased.

    this.jwts = resolveJwts(user.getJwt());
    this.authorities = authorities == null ? List.of() : authorities;
  }

  /**
   * Constructor.
   * @param id User identificator.
   * @param active Is this user active?
   * @param locked Is this user locked?
   * @param username Username.
   * @param email Email.
   * @param password Password.
   * @param authorities Authorities.
   */
  public UserLandDetails(Long id, Boolean active, Boolean locked, String username, String email, String password,
                         Set<String> jwts, Collection<? extends GrantedAuthority> authorities) {
    this.id = id;
    this.active = active;
    this.locked = locked;
    this.username = username;
    this.email = email;
    this.password = password; // Used for login validation, then erased.

    this.jwts = jwts == null ? Set.of() : jwts;
    this.authorities = authorities == null ? List.of() : authorities;
  }

  /**
   * Find out all JWTs.
   * @param jwts JWT.
   * @return Set of tokens.
   */
  private Set<String> resolveJwts(List<UserJwt> jwts) {
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
}
