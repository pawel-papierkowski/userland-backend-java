package org.portfolio.userland.config.security;

import org.springframework.boot.security.autoconfigure.actuate.web.servlet.EndpointRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration.
 */
@Configuration
public class SecurityConfig {
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
