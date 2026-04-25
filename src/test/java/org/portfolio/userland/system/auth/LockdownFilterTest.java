package org.portfolio.userland.system.auth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.portfolio.userland.system.auth.data.CustomUserDetails;
import org.portfolio.userland.system.config.service.ConfigConst;
import org.portfolio.userland.system.config.service.ConfigService;
import org.portfolio.userland.system.lockdown.exceptions.SystemLockdownException;
import org.portfolio.userland.system.lockdown.exceptions.UserLockdownException;
import org.portfolio.userland.test.helpers.context.WithMockCustomUser;
import org.portfolio.userland.test.helpers.mocks.SpringMocker;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.util.List;
import java.util.Set;

import static org.mockito.Mockito.*;

/**
 * Tests <code>LockdownFilter</code> in isolation.
 */
public class LockdownFilterTest {
  private HandlerExceptionResolver handlerExceptionResolver;
  private ConfigService configService;

  private LockdownFilter lockdownFilter;

  @BeforeEach
  void setup() {
    // Clear context amd reset mocks before each test.
    SecurityContextHolder.clearContext();
    handlerExceptionResolver = mock(HandlerExceptionResolver.class);
    configService = mock(ConfigService.class);
    PermissionService permissionService = new PermissionService();
    // LockdownFilter is real, but we need to set services it uses to our mocks.
    lockdownFilter = new LockdownFilter(configService, permissionService, handlerExceptionResolver);
  }

  // //////////////////////////////////////////////////////////////////////////

  @Test
  void inactiveLockdown() throws Exception {
    // Arrange: Setup mock HTTP request & response.
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain filterChain = new MockFilterChain();

    // Arrange: Mock services.
    when(configService.get(ConfigConst.USER_LOCKDOWN, ConfigConst.USER_LOCKDOWN_DEF)).thenReturn(ConfigConst.FALSE);

    // Act: Execute the filter.
    lockdownFilter.doFilterInternal(request, response, filterChain);

    // Assert: Filter passed.
    verifyNoInteractions(handlerExceptionResolver);
  }

  @Test
  void activeLockdown() throws Exception {
    // Arrange: Setup mock HTTP request & response.
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain filterChain = new MockFilterChain();

    // Arrange: Mock services.
    when(configService.get(ConfigConst.USER_LOCKDOWN, ConfigConst.USER_LOCKDOWN_DEF)).thenReturn(ConfigConst.TRUE);

    // Act: Execute the filter.
    lockdownFilter.doFilterInternal(request, response, filterChain);

    // Assert: Filter threw exception.
    verify(handlerExceptionResolver).resolveException(
        eq(request),
        eq(response),
        isNull(),
        any(SystemLockdownException.class)
    );
  }

  @Test
  @WithMockCustomUser
  void activeLockdownStandardUser() throws Exception {
    // Arrange: Setup mock HTTP request & response.
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain filterChain = new MockFilterChain();

    // Arrange: Setup authentication mock.
    CustomUserDetails customUserDetails = new CustomUserDetails(1L, true, false, "Jan Kowalski", "jan.kowalski@google.com", "p@S5wordN1c3", Set.of(), null);
    SpringMocker.mockAuth(customUserDetails);

    // Arrange: Mock services.
    when(configService.get(ConfigConst.USER_LOCKDOWN, ConfigConst.USER_LOCKDOWN_DEF)).thenReturn(ConfigConst.TRUE);

    // Act: Execute the filter.
    lockdownFilter.doFilterInternal(request, response, filterChain);

    // Assert: Filter threw exception.
    verify(handlerExceptionResolver).resolveException(
        eq(request),
        eq(response),
        isNull(),
        any(UserLockdownException.class)
    );
  }

  @Test
  @WithMockCustomUser(authorities = { "ROLE_ADMIN" })
  void activeLockdownAdminUser() throws Exception {
    // Arrange: Setup mock HTTP request & response.
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain filterChain = new MockFilterChain();

    // Arrange: Setup authentication mock.
    CustomUserDetails customUserDetails = new CustomUserDetails(1L, true, false, "Jan Kowalski", "jan.kowalski@google.com", "p@S5wordN1c3", Set.of(), List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    SpringMocker.mockAuth(customUserDetails);

    // Arrange: Mock services.
    when(configService.get(ConfigConst.USER_LOCKDOWN, ConfigConst.USER_LOCKDOWN_DEF)).thenReturn(ConfigConst.TRUE);

    // Act: Execute the filter.
    lockdownFilter.doFilterInternal(request, response, filterChain);

    // Assert: Filter passed.
    verifyNoInteractions(handlerExceptionResolver);
  }
}
