package org.portfolio.userland.system.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.portfolio.userland.system.auth.CustomUserDetails;
import org.portfolio.userland.system.auth.CustomUserDetailsService;
import org.portfolio.userland.system.exceptions.InvalidBearerTokenException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Service;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;

/**
 * Filter for JWT. Will always be executed, <code>addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)</code>
 * in <code>SecurityConfig</code> only changes order of filters.
 * After successful login we can access user detail data like that:
 * <pre>
 * CustomUserDetails customUserDetails = AuthHelper.resolveUserDetails();
 * </pre>
 * CustomUserDetails is null if not logged in.
 */
@Service
@RequiredArgsConstructor
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

  @Override
  protected void doFilterInternal(@NonNull HttpServletRequest request,
                                  @NonNull HttpServletResponse response,
                                  @NonNull FilterChain filterChain) throws ServletException, IOException {
    System.out.println("JwtAuthFilter.doFilterInternal() called.");
    final String authHeader = request.getHeader(HEADER_AUTH); // Get the Authorization header.
    final boolean isLockdown = false; //isLockdown(); // Find out if we have system lockdown.

    // We can be already authorized in tests using @WithMockCustomUser. Handle it separately.
    if (handleAlreadyAuth(request, response, filterChain, isLockdown)) return;

    // If header is missing or does not start with Bearer, end it. Spring should be configured so it rejects endpoints
    // that require authorization, but user do not have it.
    if (authHeader == null || !authHeader.startsWith(HEADER_TOKEN_PREFIX)) {
      // Let's continue, maybe this endpoint does not require authentication.
      filterChain.doFilter(request, response);
      return;
    }

    // Extract the token (everything after HEADER_TOKEN_PREFIX).
    final String token = authHeader.substring(HEADER_TOKEN_PREFIX_LENGTH);
    final String email;

    try {
      email = jwtService.extractEmail(token);
    } catch (Exception ex) { // Important to catch exception here!
      // Token is malformed or expired; throw specific exception.
      handlerExceptionResolver.resolveException(request, response, null, new InvalidBearerTokenException(authHeader));
      return;
    }

    // No email, something went wrong.
    if (email == null) {
      filterChain.doFilter(request, response); // Continue the filter chain.
      return;
    }

    // Load user details from database based on email, as email uniquely identifies user.
    CustomUserDetails customUserDetails = customUserDetailsService.loadUserByUsername(email);
    if (!verifyCustomUser(customUserDetails, token)) {
      filterChain.doFilter(request, response); // Continue the filter chain.
      return;
    }

    // Validate the token against the loaded user.
    if (jwtService.isTokenValid(token, customUserDetails.getEmail())) setupAuth(request, customUserDetails);
    filterChain.doFilter(request, response); // Continue the filter chain.
  }

  /**
   * Handle case when you are already authenticated. It is possible in tests.
   * @param request Request.
   * @param response Response.
   * @param filterChain Filter chain.
   * @param isLockdown Is lockdown active?
   * @return True if user is already authenticated.
   * @throws IOException If an I/O error occurs during the processing of the request.
   * @throws ServletException If the processing fails for any other reason.
   */
  private boolean handleAlreadyAuth(HttpServletRequest request,
                                 HttpServletResponse response,
                                 FilterChain filterChain,
                                 boolean isLockdown) throws ServletException, IOException {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null) return false;
    if (!(auth.getPrincipal() instanceof CustomUserDetails customUserDetails)) return false;

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
