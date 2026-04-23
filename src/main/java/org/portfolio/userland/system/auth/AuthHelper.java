package org.portfolio.userland.system.auth;

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
    if (!(principal instanceof CustomUserDetails)) return null;
    return (CustomUserDetails)principal;
  }
}
