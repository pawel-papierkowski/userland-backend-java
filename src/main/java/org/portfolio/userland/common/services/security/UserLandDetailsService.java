package org.portfolio.userland.common.services.security;

import com.google.common.collect.Lists;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.portfolio.userland.features.user.entity.Permission;
import org.portfolio.userland.features.user.entity.User;
import org.portfolio.userland.features.user.entity.UserPermission;
import org.portfolio.userland.features.user.repositories.UserRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Handles <code>UserLandDetails</code> for <code>JwtAuthFilter</code>. Usage example:
 * <pre>
 * &#064;RestController
 * &#064;RequestMapping("/api")
 * public class ProfileController { *
 *   &#064;GetMapping("/me")
 *   public String getMyProfile(@AuthenticationPrincipal UserLandDetails principal) {
 *     // You now have safe, typed access to the authenticated user's details.
 *     Long userId = principal.getId();
 *     String username = principal.getUsername();
 *     return "Hello " + username + ", your ID is " + userId;
 *   }
 * }</pre>
 */
@Service
@RequiredArgsConstructor
public class UserLandDetailsService implements UserDetailsService {
  private final UserRepository userRepository;

  @Override
  public @NonNull UserLandDetails loadUserByUsername(@NonNull String username) throws UsernameNotFoundException {
    User user = userRepository.findByEmail(username)
        .orElseThrow(() -> new UsernameNotFoundException("User '"+username+"' not found"));

    // Map permissions to authorities.
    // Example: permission "role" and userPermission "operator" will result in "ROLE_OPERATOR"
    List<GrantedAuthority> authorities = Lists.newArrayList();
    for (UserPermission userPermission : user.getPermissions()) {
      Permission permission = userPermission.getPermission();
      if (!permission.getInAuthorities()) continue;

      String authorityStr = permission.getName().toUpperCase() + "_" + userPermission.getValue().toUpperCase();
      authorities.add(new SimpleGrantedAuthority(authorityStr));
    }
    return new UserLandDetails(user, authorities);
  }
}
