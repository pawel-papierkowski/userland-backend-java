package org.portfolio.userland.common.services.jwt;

import io.jsonwebtoken.Claims;
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
  private final JwtService jwtService;
  private final UserLandDetailsService userLandDetailsService;

  @Override
  protected void doFilterInternal(@NonNull HttpServletRequest request,
                                  @NonNull HttpServletResponse response,
                                  @NonNull FilterChain filterChain) throws ServletException, IOException {
    // Get the Authorization header. JWT tokens are sent as "Bearer <token>".
    final String authHeader = request.getHeader("Authorization");

    // If header is missing or does not start with Bearer, skip this filter.
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      filterChain.doFilter(request, response);
      return;
    }

    // Extract the token (everything after "Bearer ").
    final String token = authHeader.substring(7);
    final String email;

    try {
      email = jwtService.extractClaim(token, Claims::getSubject);
    } catch (Exception ex) {
      // Token is malformed or expired; skip authentication so Spring returns 401.
      filterChain.doFilter(request, response);
      return;
    }

    // If we have an email and no authentication exists yet in the context.
    if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
      // Load user details from database.
      UserLandDetails userLand = userLandDetailsService.loadUserByUsername(email);

      // Validate the token against the loaded user.
      if (jwtService.isTokenValid(token, userLand.getEmail())) {
        // Create the authentication token containing the user, no credentials (already validated), and their roles.
        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
            userLand,
            null,
            userLand.getAuthorities()
        );

        // Attach details about the web request (like IP address, session ID).
        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        // Set the authentication in the security context.
        SecurityContextHolder.getContext().setAuthentication(authToken);
      }
    }

    // Continue the filter chain.
    filterChain.doFilter(request, response);
  }
}
