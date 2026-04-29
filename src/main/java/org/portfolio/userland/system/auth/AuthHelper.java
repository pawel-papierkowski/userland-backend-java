package org.portfolio.userland.system.auth;

import org.portfolio.userland.system.auth.details.CustomUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Authentication helper methods.
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
    // Can happen if we call endpoints that do not handle jwtAuthFilter. See SecurityConfig.
    // We treat this as not logged in.
    if (!(principal instanceof CustomUserDetails)) return null;

    return (CustomUserDetails)principal;
  }
}
