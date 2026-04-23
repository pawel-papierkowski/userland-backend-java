package org.portfolio.userland.system.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.portfolio.userland.system.auth.CustomUserDetails;
import org.portfolio.userland.system.auth.CustomUserDetailsService;
import org.portfolio.userland.system.auth.PermissionService;
import org.portfolio.userland.system.config.service.ConfigConst;
import org.portfolio.userland.system.config.service.ConfigService;
import org.portfolio.userland.system.exceptions.InvalidBearerTokenException;
import org.portfolio.userland.system.exceptions.SystemLockdownException;
import org.portfolio.userland.system.exceptions.UserLockdownException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
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
  /** JWT tokens are sent as "Bearer <token>". */
  private final static String HEADER_TOKEN_PREFIX = "Bearer ";
  /** Length of prefix above. */
  private final static int HEADER_TOKEN_PREFIX_LENGTH = HEADER_TOKEN_PREFIX.length();

  @Qualifier("handlerExceptionResolver")
  private final HandlerExceptionResolver handlerExceptionResolver;

  private final ConfigService configService;
  private final JwtService jwtService;
  private final CustomUserDetailsService customUserDetailsService;
  private final PermissionService permissionService;

  @Override
  protected void doFilterInternal(@NonNull HttpServletRequest request,
                                  @NonNull HttpServletResponse response,
                                  @NonNull FilterChain filterChain) throws ServletException, IOException {
    // Get the Authorization header.
    final String authHeader = request.getHeader(HEADER_AUTH);
    final boolean isLockdown = isLockdown();

    // If header is missing or does not start with Bearer, skip this filter. Spring should be configured
    // so it rejects endpoints that require authorization, but do not have it.
    if (authHeader == null || !authHeader.startsWith(HEADER_TOKEN_PREFIX)) {
      if (maybeThrowLockdown(isLockdown, request, response)) return;
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

    // No email or already authenticated.
    if (email == null || SecurityContextHolder.getContext().getAuthentication() != null) {
      if (maybeThrowLockdown(isLockdown, request, response)) return;
      filterChain.doFilter(request, response); // Continue the filter chain.
      return;
    }

    // Load user details from database based on email, as email uniquely identifies user.
    CustomUserDetails customUserDetails = customUserDetailsService.loadUserByUsername(email);
    if (!verifyCustomUser(customUserDetails, token)) {
      if (maybeThrowLockdown(isLockdown, customUserDetails, request, response)) return;
      filterChain.doFilter(request, response); // Continue the filter chain.
      return;
    }

    // Validate the token against the loaded user.
    if (jwtService.isTokenValid(token, customUserDetails.getEmail())) {
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

    if (maybeThrowLockdown(isLockdown, customUserDetails, request, response)) return;
    filterChain.doFilter(request, response); // Continue the filter chain.
  }

  //

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

  //

  /**
   * Checks if system is subjected to lockdown.
   * @return True if system is under lockdown, otherwise false.
   */
  private boolean isLockdown() {
    return configService.get(ConfigConst.USER_LOCKDOWN, ConfigConst.USER_LOCKDOWN_DEF).equals(ConfigConst.TRUE);
  }

  /**
   * If isLockdown is true, cause exception that will be properly handled by GlobalExceptionHandler.
   * @param isLockdown Do we have lockdown situation?
   * @param request Request.
   * @param response Response.
   * @return True if we have lockdown, otherwise false.
   */
  private boolean maybeThrowLockdown(boolean isLockdown,
                                     HttpServletRequest request,
                                     HttpServletResponse response) {
    if (!isLockdown) return false;
    handlerExceptionResolver.resolveException(request, response, null, new SystemLockdownException());
    return true;
  }

  /**
   * If isLockdown is true AND user has no access to admin panel, cause exception that will be properly handled by
   * GlobalExceptionHandler.
   * @param isLockdown Do we have lockdown situation?
   * @param customUserDetails Custom user details.
   * @param request Request.
   * @param response Response.
   * @return True if we have lockdown, otherwise false.
   */
  private boolean maybeThrowLockdown(boolean isLockdown, CustomUserDetails customUserDetails,
                                     HttpServletRequest request,
                                     HttpServletResponse response) {
    if (!isLockdown) return false;
    if (permissionService.hasAccessToAdminPanel(customUserDetails)) return false;
    handlerExceptionResolver.resolveException(request, response, null, new UserLockdownException(customUserDetails.getEmail()));
    return true;
  }
}
