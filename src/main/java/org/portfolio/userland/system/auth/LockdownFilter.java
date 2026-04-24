package org.portfolio.userland.system.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.portfolio.userland.system.config.service.ConfigConst;
import org.portfolio.userland.system.config.service.ConfigService;
import org.portfolio.userland.system.exceptions.SystemLockdownException;
import org.portfolio.userland.system.exceptions.UserLockdownException;
import org.portfolio.userland.system.jwt.JwtAuthFilter;
import org.springframework.beans.factory.annotation.Qualifier;
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

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
    System.out.println("LockdownFilter.doFilterInternal() called.");
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
    if (permissionService.hasAccessToAdminPanel()) { // Admin users are allowed in.
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
