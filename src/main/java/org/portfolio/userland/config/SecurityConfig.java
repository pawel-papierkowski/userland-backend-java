package org.portfolio.userland.config;

import org.springframework.boot.security.autoconfigure.actuate.web.servlet.EndpointRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import java.util.UUID;

/**
 * Security configuration.
 */
@Configuration
@EnableMethodSecurity
public class SecurityConfig {
  /**
   * Defines password encoder bean.
   * @return Password encoder bean.
   */
  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(); // we can safely hash passwords with this
  }

  /**
   * Create a hardcoded Spring test user with a random UUID as password.
   * @param passwordEncoder Password encoder bean.
   * @return User details service.
   */
  @Bean
  public UserDetailsService userDetailsService(PasswordEncoder passwordEncoder) {
    UserDetails testUser = User.builder()
        .username("admin")
        .password(passwordEncoder.encode(UUID.randomUUID().toString()))
        .roles("ADMIN")
        .build();
    return new InMemoryUserDetailsManager(testUser);
  }

  //

  /**
   * This specific filter chain only applies to /actuator/** endpoints.
   * Since it is portfolio project, we can open it to everyone.
   * Note: config in YML will restrict endpoints to ones that are safe.
   * @param http HTTP security data.
   * @return Security filter chain.
   */
  @Bean
  @Order(1)
  public SecurityFilterChain actuatorSecurityFilterChain(HttpSecurity http) {
    // EndpointRequest knows where actuator endpoints are.
    http.csrf(AbstractHttpConfigurer::disable)
        .securityMatcher(EndpointRequest.toAnyEndpoint())
        .authorizeHttpRequests(requests -> requests.anyRequest().permitAll());
    return http.build();
  }

  /**
   * This specific filter chain only applies to Swagger/OpenAPI endpoints.
   * Since it is portfolio project, we can open it to everyone.
   * @param http HTTP security data.
   * @return Security filter chain.
   */
  @Bean
  @Order(2)
  public SecurityFilterChain swaggerSecurityFilterChain(HttpSecurity http) {
    http.csrf(AbstractHttpConfigurer::disable)
        .securityMatcher("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
        .authorizeHttpRequests(requests -> requests.anyRequest().permitAll());
    return http.build();
  }

  /**
   * This specific filter chain only applies to user endpoints.
   * By nature of register/confirm/login/etc endpoints, these must be available publicly to everyone.
   * @param http HTTP security data.
   * @return Security filter chain.
   */
  @Bean
  @Order(3)
  public SecurityFilterChain userSecurityFilterChain(HttpSecurity http) {
    http.csrf(AbstractHttpConfigurer::disable)
        .securityMatcher(
            "/api/users/register", // user registration
            "/api/users/activate", // activate user account
            "/api/users/password/*", // reset password
            "/api/users/delete/*", // delete account
            "/api/users/login") // login user
        .authorizeHttpRequests(requests -> requests.anyRequest().permitAll());
    return http.build();
  }

  /**
   * This is default security chain. API of actual application and other stuff is handled above.
   * @param http HTTP security data.
   * @return Security filter chain.
   */
  @Bean
  @Order(100) // make sure it is last
  public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) {
    // Anything that was not caught by security filter chains above will require authentication.
    http.csrf(AbstractHttpConfigurer::disable)
        .authorizeHttpRequests(requests -> requests.anyRequest().authenticated());
    return http.build();
  }
}
