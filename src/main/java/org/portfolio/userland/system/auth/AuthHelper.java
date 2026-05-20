package org.portfolio.userland.system.auth;

import org.portfolio.userland.config.security.SecurityConfig;
import org.portfolio.userland.system.auth.details.CustomUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Authentication helper methods.
 * @see SecurityConfig
 */
public class AuthHelper {
  /**
   * Resolve custom user details from authentication data.
   * @return Custom user details or null if no authentication is present.
   */
  public static CustomUserDetails resolveUserDetails() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()) return null;

    Object principal = authentication.getPrincipal();
    if (!(principal instanceof CustomUserDetails)) {
      // Can happen if we call endpoints that do not handle jwtAuthFilter. See SecurityConfig.
      // We treat this as not logged in.
      return null;
    }

    return (CustomUserDetails)principal;
  }
}
