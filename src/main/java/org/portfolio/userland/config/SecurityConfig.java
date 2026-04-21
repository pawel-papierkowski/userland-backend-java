package org.portfolio.userland.config;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.portfolio.userland.common.services.jwt.JwtAuthFilter;
import org.portfolio.userland.common.services.security.UserLandDetailsService;
import org.springframework.boot.security.autoconfigure.actuate.web.servlet.EndpointRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Security configuration.
 * <p>Major part of security configuration is endpoint config. We assume all endpoints require authentication unless
 * specified otherwise. In particular, <code>publicSecurityFilterChain()</code> and <code>availableSecurityFilterChain()</code>
 * specify endpoints that do not require authentication.</p>
 */
@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
  private final UserLandDetailsService userLandDetailsService;
  private final JwtAuthFilter jwtAuthFilter;

  /**
   * Defines password encoder bean.
   * @return Password encoder bean.
   */
  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(); // we can safely hash passwords with this
  }

  @Bean
  public DaoAuthenticationProvider authenticationProvider(PasswordEncoder passwordEncoder) {
    DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userLandDetailsService);
    provider.setPasswordEncoder(passwordEncoder);
    return provider;
  }

  //

  /**
   * This specific filter chain only applies to /actuator/** endpoints. Since it is portfolio project, we can open it to
   * everyone. Note: config in YML will restrict endpoints to ones that are safe.
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
   * This specific filter chain only applies to Swagger/OpenAPI endpoints. Since it is portfolio project, we can open it
   * to everyone.
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
   * This specific filter chain defines public endpoints. Note these won't have auth data even if you provide token.
   * By nature of register/activate/login/etc endpoints, these must be available publicly to everyone.
   * @param http HTTP security data.
   * @return Security filter chain.
   */
  @Bean
  @Order(3)
  public SecurityFilterChain publicSecurityFilterChain(HttpSecurity http) {
    http.csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .securityMatcher(
            "/api/checks/alive", // alive check
            "/api/users/register", // user registration
            "/api/users/activate", // activate user account
            "/api/users/password/*", // reset password
            "/api/users/delete/*", // delete account
            "/api/users/login") // login user
        .authorizeHttpRequests(requests -> requests.anyRequest().permitAll());
    return http.build();
  }

  /**
   * This specific filter chain defines public endpoints that can also be accessed while being logged in.
   * You have access to auth data inside (if JWT was provided, otherwise principal will be anonymous user).
   * @param http HTTP security data.
   * @return Security filter chain.
   */
  @Bean
  @Order(4)
  public SecurityFilterChain availableSecurityFilterChain(HttpSecurity http) {
    http.csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .securityMatcher(
            "/api/users/logout") // logout user
        .authorizeHttpRequests(requests -> requests.anyRequest().permitAll())
        .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
    return http.build();
  }

  /**
   * This is default security chain. All endpoints here are expected to have JWT in header, otherwise request
   * will be rejected with 401 Unauthorized. You have access to auth data inside.
   * @param http HTTP security data.
   * @return Security filter chain.
   */
  @Bean
  @Order(100) // make sure it is last
  public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) {
    // Anything that was not caught by security filter chains above will require authentication.
    // We do exceptionHandling() to enforce 401 if there is no token and 403 if access is denied. Yes, these are two
    // different things.
    http.csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(requests -> requests.anyRequest().authenticated())
        .exceptionHandling(ex -> ex
            .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
            .accessDeniedHandler((request, response, _) ->
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Forbidden"))
        )
        .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
    return http.build();
  }
}
