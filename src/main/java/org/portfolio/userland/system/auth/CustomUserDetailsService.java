package org.portfolio.userland.system.auth;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.portfolio.userland.features.user.entities.User;
import org.portfolio.userland.features.user.repositories.UserRepository;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Handles <code>CustomUserDetails</code> for <code>JwtAuthFilter</code>.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
  private final UserRepository userRepository;

  /**
   * Locates the user based on the username. Note: username is email.
   * @param email The username identifying the user whose data is required.
   * @return User details.
   * @throws UsernameNotFoundException When user was not found.
   */
  @Override
  @Transactional
  public @NonNull CustomUserDetails loadUserByUsername(@NonNull String email) throws UsernameNotFoundException {
    // Note we use special version of findByEmail that eagerly loads permissions and jwt as they are always used
    // in CustomUserDetails.
    User user = userRepository.findAuthByEmail(email)
        .orElseThrow(() -> new UsernameNotFoundException("User '"+email+"' not found"));
    return new CustomUserDetails(user);
  }
}
