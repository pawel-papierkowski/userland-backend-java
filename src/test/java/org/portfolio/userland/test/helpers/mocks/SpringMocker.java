package org.portfolio.userland.test.helpers.mocks;

import org.portfolio.userland.system.auth.data.CustomUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Provides mocks for Spring stuff.
 */
public class SpringMocker {
  /**
   * Manually mock Authentication so it returns given custom user details.
   * @param customUserDetails Custom user details.
   */
  public static void mockAuth(CustomUserDetails customUserDetails) {
    // Mock the Authentication object.
    Authentication authentication = mock(Authentication.class);
    when(authentication.isAuthenticated()).thenReturn(true);
    when(authentication.getPrincipal()).thenReturn(customUserDetails);

    // Mock the SecurityContext.
    SecurityContext securityContext = mock(SecurityContext.class);
    when(securityContext.getAuthentication()).thenReturn(authentication);

    // Set the mocked context into the SecurityContextHolder.
    SecurityContextHolder.setContext(securityContext);
  }
}
