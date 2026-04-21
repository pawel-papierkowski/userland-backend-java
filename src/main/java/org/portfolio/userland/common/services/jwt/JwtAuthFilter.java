package org.portfolio.userland.common.services.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.portfolio.userland.common.services.security.UserLandDetails;
import org.portfolio.userland.common.services.security.UserLandDetailsService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Service;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filter for JWT.
 */
@Service
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {
  /** Header used for authorization. */
  private final static String HEADER_AUTH = "Authorization";
  /** JWT tokens are sent as "Bearer <token>". */
  private final static String HEADER_TOKEN_PREFIX = "Bearer ";
  /** Length of prefix above. */
  private final static int HEADER_TOKEN_PREFIX_LENGTH = HEADER_TOKEN_PREFIX.length();

  private final JwtService jwtService;
  private final UserLandDetailsService userLandDetailsService;

  @Override
  protected void doFilterInternal(@NonNull HttpServletRequest request,
                                  @NonNull HttpServletResponse response,
                                  @NonNull FilterChain filterChain) throws ServletException, IOException {
    // Get the Authorization header.
    final String authHeader = request.getHeader(HEADER_AUTH);

    // If header is missing or does not start with Bearer, skip this filter. Spring should be configured
    // so it rejects endpoints that require authorization, but do not have it.
    if (authHeader == null || !authHeader.startsWith(HEADER_TOKEN_PREFIX)) {
      filterChain.doFilter(request, response);
      return;
    }

    // Extract the token (everything after HEADER_TOKEN_PREFIX).
    final String token = authHeader.substring(HEADER_TOKEN_PREFIX_LENGTH);
    final String email;

    try {
      email = jwtService.extractEmail(token);
    } catch (Exception ex) { // Important to catch exception here!
      // Token is malformed or expired; skip authentication so Spring returns 401.
      filterChain.doFilter(request, response);
      return;
    }

    // No email or already authenticated.
    if (email == null || SecurityContextHolder.getContext().getAuthentication() != null) {
      filterChain.doFilter(request, response); // Continue the filter chain.
      return;
    }

    // Load user details from database based on email, as email uniquely identifies user.
    UserLandDetails userLandDetails = userLandDetailsService.loadUserByUsername(email);
    if (!verifyUserLand(userLandDetails, token)) {
      filterChain.doFilter(request, response); // Continue the filter chain.
      return;
    }

    // Validate the token against the loaded user.
    if (jwtService.isTokenValid(token, userLandDetails.getEmail())) {
      // Create the authentication token containing the user, no credentials (already validated), and their roles.
      UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
          userLandDetails,
          null,
          userLandDetails.getAuthorities()
      );

      // Attach details about the web request (like IP address, session ID).
      authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

      // Set the authentication in the security context.
      SecurityContextHolder.getContext().setAuthentication(authToken);
    }

    filterChain.doFilter(request, response); // Continue the filter chain.
  }

  /**
   * Verify if user can be logged in.
   * @param userLandDetails UserLand details.
   * @param jwtStr Token string for JWT.
   * @return True if user can be logged in, otherwise false.
   */
  private boolean verifyUserLand(UserLandDetails userLandDetails, String jwtStr) {
    // Check if token is present in database (not revoked).
    if (!userLandDetails.getJwts().contains(jwtStr)) return false;
    // Other checks.
    return userLandDetails.getActive() && !userLandDetails.getLocked();
  }
}
