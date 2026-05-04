package org.portfolio.userland.config.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Auxiliary security configuration. Needed to prevent circular dependencies and other unpleasantness.
 */
@Configuration
@RequiredArgsConstructor
public class SecurityAuxConfig {
  /**
   * Defines password encoder bean.
   * @return Password encoder bean.
   */
  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(); // we can safely hash passwords with this
  }
}
