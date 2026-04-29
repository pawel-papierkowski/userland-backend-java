package org.portfolio.userland.system.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.NonNull;
import org.portfolio.userland.system.auth.details.CustomUserDetails;
import org.portfolio.userland.system.auth.jwt.JwtAuthFilter;
import org.portfolio.userland.system.auth.perm.EnPermKind;
import org.portfolio.userland.system.auth.perm.PermissionService;
import org.portfolio.userland.system.config.service.ConfigConst;
import org.portfolio.userland.system.config.service.ConfigService;
import org.portfolio.userland.system.lockdown.exceptions.SystemLockdownException;
import org.portfolio.userland.system.lockdown.exceptions.UserLockdownException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.stereotype.Service;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;

/**
 * <p>Checks if we have lockdown situation and reacts accordingly.</p>
 * <p>Notes:</p>
 * <ul>
 *   <li>Ensure this filter happens after <code>JwtAuthFilter</code> in <code>SecurityConfig</code>.</li>
 *   <li>Users with admin permissions are exempt from lockdown.</li>
 * </ul>
 * @see JwtAuthFilter
 */
@Service
@RequiredArgsConstructor
public class LockdownFilter extends OncePerRequestFilter {
  private final ConfigService configService;
  private final PermissionService permissionService;

  @Qualifier("handlerExceptionResolver")
  private final HandlerExceptionResolver handlerExceptionResolver;

  /** Login endpoint matcher.*/
  private final RequestMatcher loginMatcher = PathPatternRequestMatcher.withDefaults().matcher("/api/users/login");

  @Override
  protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
    // Login endpoint is exempt - lockdown is checked separately after confirming credentials of user and their
    // permissions. Reason is that we want to allow users with appropriate permissions to login even during lockdown.
    return loginMatcher.matches(request);
  }

  @Override
  protected void doFilterInternal(@NonNull HttpServletRequest request,
                                  @NonNull HttpServletResponse response,
                                  @NonNull FilterChain filterChain) throws ServletException, IOException {
    final boolean isLockdown = isLockdown(); // Find out if we have system lockdown.
    if (!isLockdown) { // No lockdown, end it now.
      filterChain.doFilter(request, response);
      return;
    }

    CustomUserDetails customUserDetails = AuthHelper.resolveUserDetails();
    if (customUserDetails == null) { // Not logged in for whatever reason.
      throwLockdown(request, response, null);
      return;
    }

    // We have system lockdown, but there are exceptions!
    if (permissionService.has(EnPermKind.ACCESS_TO_ADMIN_PANEL)) { // Admin users are allowed in.
      filterChain.doFilter(request, response);
      return;
    }

    // Reject.
    throwLockdown(request, response, customUserDetails.getEmail());
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
   * Cause system lockdown exception that will be properly handled by <code>GlobalExceptionHandler</code>.
   * @param request Request.
   * @param response Response.
   * @param email User email. Can be null or empty.
   */
  private void throwLockdown(HttpServletRequest request, HttpServletResponse response, String email) {
    if (StringUtils.isEmpty(email)) handlerExceptionResolver.resolveException(request, response, null, new SystemLockdownException());
    else handlerExceptionResolver.resolveException(request, response, null, new UserLockdownException(email));
  }
}
