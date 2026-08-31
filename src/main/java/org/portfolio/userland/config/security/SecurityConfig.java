package org.portfolio.userland.config.security;

import lombok.RequiredArgsConstructor;
import org.portfolio.userland.config.security.constants.CorsConst;
import org.portfolio.userland.config.security.constants.EndpointConst;
import org.portfolio.userland.system.auth.LockdownFilter;
import org.portfolio.userland.system.auth.RateLimitFilter;
import org.portfolio.userland.system.auth.jwt.JwtAuthFilter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.security.autoconfigure.actuate.web.servlet.EndpointRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * Security configuration.
 * <p>Major part of security configuration is endpoint config. We assume all endpoints require authentication unless
 * specified otherwise. In particular, <code>publicSecurityFilterChain()</code> specify endpoints that do not require authentication.</p>
 * <p>Note: filters are always applied, code like <code>addFilterBefore()</code> only determine changes in order of filters.</p>
 */
@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
  /** Google's OIDC issuer for ID tokens. */
  private static final String GOOGLE_ISSUER = "https://accounts.google.com";
  /** Google's public JWKS endpoint - holds public keys used to verify token signatures. */
  private static final String GOOGLE_JWKS_URI = "https://www.googleapis.com/oauth2/v3/certs";

  private final JwtAuthFilter jwtAuthFilter;
  private final LockdownFilter lockdownFilter;
  private final ObjectProvider<RateLimitFilter> rateLimitFilterProvider;
  private final JwtGcpTokenValidator jwtGcpTokenValidator;
  private final ProblemDetailAuthenticationEntryPoint problemDetailAuthenticationEntryPoint;
  private final ProblemDetailAccessDeniedHandler problemDetailAccessDeniedHandler;

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
    http.cors(Customizer.withDefaults())
        .csrf(AbstractHttpConfigurer::disable)
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
    http.cors(Customizer.withDefaults())
        .csrf(AbstractHttpConfigurer::disable)
        .securityMatcher(EndpointConst.SWAGGER)
        .authorizeHttpRequests(requests -> requests.anyRequest().permitAll());
    addRateLimitFilter(http);
    return http.build();
  }

  /**
   * This specific filter chain defines public endpoints.
   * By nature of register/activate/login/etc endpoints, these must be available publicly to everyone.
   * @param http HTTP security data.
   * @return Security filter chain.
   */
  @Bean
  @Order(3)
  public SecurityFilterChain publicSecurityFilterChain(HttpSecurity http) {
    http.cors(Customizer.withDefaults())
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .securityMatcher(EndpointConst.PUBLIC)
        .authorizeHttpRequests(requests -> requests.anyRequest().permitAll())
        .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
        .addFilterAfter(lockdownFilter, JwtAuthFilter.class);
    addRateLimitFilter(http);
    return http.build();
  }

  /**
   * This specific filter chain defines secured endpoints that also requires operator or administrator permissions.
   * Note there can be also endpoints individually marked as <code>@PreAuthorize("hasAuthority('ROLE_OPERATOR')")</code>
   * in Controllers.
   * <p>You have access to auth data inside such endpoints:</p>
   * <pre>CustomUserDetails userDetails = AuthHelper.resolveUserDetails();</pre>
   * @param http HTTP security data.
   * @return Security filter chain.
   */
  @Bean
  @Order(10)
  public SecurityFilterChain operatorSecurityFilterChain(HttpSecurity http) {
    http.cors(Customizer.withDefaults())
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .securityMatcher(EndpointConst.ADMIN)
        .authorizeHttpRequests(requests -> requests.anyRequest().hasAnyAuthority("ROLE_OPERATOR", "ROLE_ADMIN"))
        .exceptionHandling(ex -> ex
            .authenticationEntryPoint(problemDetailAuthenticationEntryPoint)
            .accessDeniedHandler(problemDetailAccessDeniedHandler)
        )
        .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
        .addFilterAfter(lockdownFilter, JwtAuthFilter.class);
    addRateLimitFilter(http);
    return http.build();
  }

  /**
   * This specific filter chain defines secured endpoints that also requires administrator permissions.
   * Note there can be also endpoints individually marked as <code>@HasAdminPermission</code> in Controllers.
   * <p>You have access to auth data inside such endpoints:</p>
   * <pre>CustomUserDetails userDetails = AuthHelper.resolveUserDetails();</pre>
   * @param http HTTP security data.
   * @return Security filter chain.
   */
  @Bean
  @Order(11)
  public SecurityFilterChain adminSecurityFilterChain(HttpSecurity http) {
    http.cors(Customizer.withDefaults())
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .securityMatcher(EndpointConst.SYSTEM) // any system endpoint
        .authorizeHttpRequests(requests -> requests.anyRequest().hasAuthority("ROLE_ADMIN"))
        .exceptionHandling(ex -> ex
            .authenticationEntryPoint(problemDetailAuthenticationEntryPoint)
            .accessDeniedHandler(problemDetailAccessDeniedHandler)
        )
        .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
        .addFilterAfter(lockdownFilter, JwtAuthFilter.class);
    addRateLimitFilter(http);
    return http.build();
  }

  /**
   * This specific filter chain defines endpoints called by Google Cloud (e.g. Cloud Tasks).
   * It requires a valid Google-signed OIDC token that additionally passes {@link JwtGcpTokenValidator} - meaning
   * the token must target our endpoint (audience) and must be minted for our service account (identity). Without
   * these extra checks, any Google-signed token from any account would be accepted.
   * @param http HTTP security data.
   * @param gcpJwtDecoder JWT decoder for verifying GCP tokens.
   * @return Security filter chain.
   */
  @Bean
  @Order(50) // Make sure it's before the defaultSecurityFilterChain
  public SecurityFilterChain gcpInternalSecurityFilterChain(HttpSecurity http, JwtDecoder gcpJwtDecoder) {
    http.cors(Customizer.withDefaults())
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .securityMatcher(EndpointConst.GCP) // Match all GCP endpoints
        .authorizeHttpRequests(requests -> requests.anyRequest().authenticated())
        // Enable OAuth2 Resource Server to automatically validate the Bearer token against Google's public keys,
        // plus our own extra validation (audience + service account identity).
        .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.decoder(gcpJwtDecoder)))
        .exceptionHandling(ex -> ex
            .authenticationEntryPoint(problemDetailAuthenticationEntryPoint)
            .accessDeniedHandler(problemDetailAccessDeniedHandler)
        );

    return http.build();
  }

  /**
   * Builds JWT decoder used to verify tokens calling GCP endpoints.
   * <p>Decoder verifies signature against Google's public keys and delegates remaining checks (timestamp, issuer,
   * and our custom audience/identity validation) to a chain of validators.</p>
   * <p>Note: JWKS URI is hardcoded instead of derived from issuer metadata, so no HTTP call is made at startup.</p>
   * @return JWT decoder for GCP tokens.
   */
  @Bean("gcpJwtDecoder")
  public JwtDecoder gcpJwtDecoder() {
    NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(GOOGLE_JWKS_URI).build();
    decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
        new JwtTimestampValidator(), // standard: reject expired/too-early tokens
        new JwtIssuerValidator(GOOGLE_ISSUER), // token must come from Google
        jwtGcpTokenValidator // our extra checks: audience + service account identity
    ));
    return decoder;
  }

  /**
   * This is default security chain. All endpoints here are expected to have JWT in header, otherwise request
   * will be rejected with 401 Unauthorized. No special permissions needed.
   * <p>You have access to auth data inside such endpoints:</p>
   * <pre>CustomUserDetails userDetails = AuthHelper.resolveUserDetails();</pre>
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
    http.cors(Customizer.withDefaults())
        .csrf(AbstractHttpConfigurer::disable)
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(requests -> requests.anyRequest().authenticated())
        .exceptionHandling(ex -> ex
            .authenticationEntryPoint(problemDetailAuthenticationEntryPoint)
            .accessDeniedHandler(problemDetailAccessDeniedHandler)
        )
        .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
        .addFilterAfter(lockdownFilter, JwtAuthFilter.class);
    addRateLimitFilter(http);
    return http.build();
  }

  // SUPPORTING BEANS

  /**
   * Conditionally adds the rate limit filter before the JWT auth filter.
   * <p>If {@link RateLimitFilter} is not available (e.g. in slice tests), this is a no-op.</p>
   * @param http HTTP security data.
   */
  private void addRateLimitFilter(HttpSecurity http) {
    RateLimitFilter rl = rateLimitFilterProvider.getIfAvailable();
    if (rl != null) http.addFilterBefore(rl, UsernamePasswordAuthenticationFilter.class);
  }

  /**
   * Configures CORS so frontend can work with backend without CORS-related issues.
   * We define CORS config here and not in separate <code>WebConfig</code>, because otherwise some requests will fail
   * before ever reaching <code>WebConfig</code>.
   * <p>In filter chains, <code>Customizer.withDefaults()</code> will effectively use config bean configured here.</p>
   * @return CORS configuration source.
   */
  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setMaxAge(3600L);
    configuration.setAllowedOrigins(Arrays.asList(CorsConst.ALLOWED_ORIGINS));
    configuration.setAllowedMethods(Arrays.asList(CorsConst.ALLOWED_METHODS));
    configuration.setAllowedHeaders(Arrays.asList(CorsConst.ALLOWED_HEADERS));
    configuration.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }
}
