package org.portfolio.userland.system.auth.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.portfolio.userland.config.security.SecurityConfig;
import org.portfolio.userland.config.security.constants.EndpointConst;
import org.portfolio.userland.system.auth.details.CustomUserDetails;
import org.portfolio.userland.system.auth.details.CustomUserDetailsService;
import org.portfolio.userland.system.auth.jwt.exceptions.InvalidBearerTokenException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Service;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Filter for JWT. Will always be executed, <code>addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)</code>
 * in <code>SecurityConfig</code> only changes order of filters.
 * After successful login we can access user detail data like that:
 * <pre>
 * CustomUserDetails customUserDetails = AuthHelper.resolveUserDetails();
 * </pre>
 * CustomUserDetails is null if not logged in.
 * <p>Notes:</p>
 * <ul>
 *   <li>GCP endpoints are exempt from this filter, as they are secured separately via OIDC token.</li>
 * </ul>
 *
 * @see SecurityConfig
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JwtAuthFilter extends OncePerRequestFilter {
  /** Header used for authorization. */
  private final static String HEADER_AUTH = "Authorization";
  /** JWT tokens are sent in authorization header as "Bearer [token]". */
  private final static String HEADER_TOKEN_PREFIX = "Bearer ";
  /** Length of prefix above. */
  private final static int HEADER_TOKEN_PREFIX_LENGTH = HEADER_TOKEN_PREFIX.length();

  private final JwtService jwtService;
  private final CustomUserDetailsService customUserDetailsService;

  @Qualifier("handlerExceptionResolver")
  private final HandlerExceptionResolver handlerExceptionResolver;

  //

  /** Exempt endpoint matcher. */
  private final RequestMatcher exemptMatcher = new OrRequestMatcher(
      PathPatternRequestMatcher.withDefaults().matcher("/api/gcp/**")
  );

  /** Public endpoint matcher. */
  private final RequestMatcher publicEndpointsMatcher = new OrRequestMatcher(
      Arrays.stream(EndpointConst.PUBLIC)
          .map(PathPatternRequestMatcher.withDefaults()::matcher)
          .collect(Collectors.toList())
  );

  @Override
  protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
    // Certain endpoints are exempt from this filter.
    return exemptMatcher.matches(request);
  }

  //

  @Override
  protected void doFilterInternal(@NonNull HttpServletRequest request,
                                  @NonNull HttpServletResponse response,
                                  @NonNull FilterChain filterChain) throws ServletException, IOException {
    log.trace("Processing JWT for endpoint: {} {}", request.getMethod(), request.getRequestURI());

    final String authHeader = request.getHeader(HEADER_AUTH); // Get the Authorization header.

    // We can be already authenticated/authorized in tests. Handle it separately.
    if (handleAlreadyAuth(request, response, filterChain)) return;

    // If header is missing or does not start with Bearer, end it. SecurityConfig handles cases when endpoint
    // requires authentication/authorization, but it is not provided.
    if (authHeader == null || !authHeader.startsWith(HEADER_TOKEN_PREFIX)) {
      log.trace("No token detected.");
      // Let's continue, maybe this endpoint does not require authentication.
      filterChain.doFilter(request, response);
      return;
    }

    // Extract the token (everything after HEADER_TOKEN_PREFIX).
    final String token = authHeader.substring(HEADER_TOKEN_PREFIX_LENGTH);
    log.trace("Token found: {}", token);

    final String email;
    try {
      email = jwtService.extractEmail(token);
    } catch (Exception ex) { // Important to catch exception here!
      // If it's a public endpoint, ignore the JWT failure and proceed without authentication.
      if (publicEndpointsMatcher.matches(request)) {
        log.trace("Token is malformed or expired, but endpoint is public.");
        filterChain.doFilter(request, response);
        return;
      }
      // Token is malformed or expired; throw specific exception for non-public endpoints.
      log.trace("Token is malformed or expired.");
      handlerExceptionResolver.resolveException(request, response, null, new InvalidBearerTokenException(authHeader));
      return;
    }

    // No email, something went wrong.
    if (email == null) {
      log.trace("Something went wrong when trying to use token.");
      filterChain.doFilter(request, response); // Continue the filter chain.
      return;
    }

    // Load user details from database based on email, as email uniquely identifies user.
    CustomUserDetails customUserDetails = customUserDetailsService.loadUserByUsername(email);
    if (!verifyCustomUser(customUserDetails, token)) {
      log.trace("Failed to verify custom user details.");
      filterChain.doFilter(request, response); // Continue the filter chain.
      return;
    }

    // Validate the token against the loaded user.
    if (jwtService.isTokenValid(token, customUserDetails.getEmail())) {
      setupAuth(request, customUserDetails);
      log.trace("Successfully authenticated user: {}.", customUserDetails.getEmail());
    }
    filterChain.doFilter(request, response); // Continue the filter chain.
  }

  /**
   * Handle case when you are already authenticated. It is possible in tests using @WithMockCustomUser.
   * @param request Request.
   * @param response Response.
   * @param filterChain Filter chain.
   * @return True if user is already authenticated.
   * @throws IOException If an I/O error occurs during the processing of the request.
   * @throws ServletException If the processing fails for any other reason.
   */
  private boolean handleAlreadyAuth(HttpServletRequest request,
                                 HttpServletResponse response,
                                 FilterChain filterChain) throws ServletException, IOException {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null) return false;
    if (!(auth.getPrincipal() instanceof CustomUserDetails)) return false;

    filterChain.doFilter(request, response); // Continue the filter chain.
    return true;
  }

  /**
   * Actually set up authentification. Finally.
   * @param request Request.
   * @param customUserDetails Custom user details resolved from token string and database user.
   */
  private void setupAuth(HttpServletRequest request, CustomUserDetails customUserDetails) {
    // Create the authentication token containing the user, no credentials (already validated), and their roles.
    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
        customUserDetails,
        null,
        customUserDetails.getAuthorities()
    );

    // Attach details about the web request (like IP address, session ID).
    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

    // Set the authentication in the security context.
    SecurityContextHolder.getContext().setAuthentication(authToken);
  }

  // //////////////////////////////////////////////////////////////////////////

  /**
   * Verify if user can be logged in.
   * @param customUserDetails Custom user details.
   * @param jwtStr Token string for JWT.
   * @return True if user can be logged in, otherwise false.
   */
  private boolean verifyCustomUser(CustomUserDetails customUserDetails, String jwtStr) {
    // Check if JWT is present in database (not revoked).
    if (!customUserDetails.getJwts().contains(jwtStr)) return false;
    // Other checks.
    return customUserDetails.getActive() && !customUserDetails.getLocked();
  }
}