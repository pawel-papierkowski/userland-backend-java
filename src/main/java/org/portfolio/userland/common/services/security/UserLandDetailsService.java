package org.portfolio.userland.common.services.security;

import com.google.common.collect.Lists;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.portfolio.userland.features.user.entities.Permission;
import org.portfolio.userland.features.user.entities.User;
import org.portfolio.userland.features.user.entities.UserPermission;
import org.portfolio.userland.features.user.repositories.UserRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Handles <code>UserLandDetails</code> for <code>JwtAuthFilter</code>.
 */
@Service
@RequiredArgsConstructor
public class UserLandDetailsService implements UserDetailsService {
  private final UserRepository userRepository;

  /**
   * Locates the user based on the username. Note: username is email.
   * @param email The username identifying the user whose data is required.
   * @return User details.
   * @throws UsernameNotFoundException When user was not found.
   */
  @Override
  @Transactional
  public @NonNull UserLandDetails loadUserByUsername(@NonNull String email) throws UsernameNotFoundException {
    User user = userRepository.findByEmail(email)
        .orElseThrow(() -> new UsernameNotFoundException("User '"+email+"' not found"));

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
