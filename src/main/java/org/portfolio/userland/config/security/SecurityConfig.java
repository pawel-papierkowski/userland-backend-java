package org.portfolio.userland.config.security;

import lombok.RequiredArgsConstructor;
import org.portfolio.userland.system.jwt.JwtAuthFilter;
import org.springframework.boot.security.autoconfigure.actuate.web.servlet.EndpointRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
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
  private final JwtAuthFilter jwtAuthFilter;
  private final ProblemDetailAuthenticationEntryPoint problemDetailAuthenticationEntryPoint;
  private final ProblemDetailAccessDeniedHandler problemDetailAccessDeniedHandler;

  /**
   * Defines password encoder bean.
   * @return Password encoder bean.
   */
  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(); // we can safely hash passwords with this
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
        .authorizeHttpRequests(requests -> requests.anyRequest().permitAll())
        .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
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
   * This specific filter chain defines secured endpoints that also requires operator or administrator permissions.
   * Note there can be endpoints individually marked as <code>@PreAuthorize("hasAuthority('ROLE_OPERATOR')")</code>.
   * @param http HTTP security data.
   * @return Security filter chain.
   */
  @Bean
  @Order(10)
  public SecurityFilterChain operatorSecurityFilterChain(HttpSecurity http) {
    http.csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .securityMatcher("/api/admin/*") // any administration panel endpoint
        .authorizeHttpRequests(requests -> requests.anyRequest().hasAnyAuthority("ROLE_OPERATOR", "ROLE_ADMIN"))
        .exceptionHandling(ex -> ex
            .authenticationEntryPoint(problemDetailAuthenticationEntryPoint)
            .accessDeniedHandler(problemDetailAccessDeniedHandler)
        )
        .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
    return http.build();
  }

  /**
   * This specific filter chain defines secured endpoints that also requires administrator permissions.
   * Note there can be endpoints individually marked as <code>@PreAuthorize("hasAuthority('ROLE_ADMIN')")</code>.
   * @param http HTTP security data.
   * @return Security filter chain.
   */
  @Bean
  @Order(11)
  public SecurityFilterChain adminSecurityFilterChain(HttpSecurity http) {
    http.csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .securityMatcher("/api/system/*") // any system endpoint
        .authorizeHttpRequests(requests -> requests.anyRequest().hasAuthority("ROLE_ADMIN"))
        .exceptionHandling(ex -> ex
            .authenticationEntryPoint(problemDetailAuthenticationEntryPoint)
            .accessDeniedHandler(problemDetailAccessDeniedHandler)
        )
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
    // Anything that was not caught by security filter chains above will require standard authentication without any
    // special permissions.
    // In exceptionHandling() we do custom handling so GlobalExceptionHandler can process these errors and return
    // correct problem detail.
    // - authenticationEntryPoint() enforce 401 if there is no token
    // - accessDeniedHandler() enforce 403 if access is denied (no permission).
    http.csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(requests -> requests.anyRequest().authenticated())
        .exceptionHandling(ex -> ex
            .authenticationEntryPoint(problemDetailAuthenticationEntryPoint)
            .accessDeniedHandler(problemDetailAccessDeniedHandler)
        )
        .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
    return http.build();
  }
}
