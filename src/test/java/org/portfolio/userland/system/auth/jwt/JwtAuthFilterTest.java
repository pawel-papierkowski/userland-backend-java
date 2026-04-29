package org.portfolio.userland.system.auth.jwt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.portfolio.userland.system.auth.details.CustomUserDetails;
import org.portfolio.userland.system.auth.details.CustomUserDetailsService;
import org.portfolio.userland.system.auth.jwt.exceptions.InvalidBearerTokenException;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests <code>JwtAuthFilter</code> in isolation.
 */
public class JwtAuthFilterTest {
  private final static String TOKEN_VALID = "valid.jwt.token";
  private static final String TOKEN_BAD = "bad.jwt.token";

  private HandlerExceptionResolver handlerExceptionResolver;
  private JwtService jwtService;
  private CustomUserDetailsService customUserDetailsService;

  private JwtAuthFilter jwtAuthFilter;

  @BeforeEach
  void setup() {
    // Clear context amd reset mocks before each test.
    SecurityContextHolder.clearContext();

    handlerExceptionResolver = mock(HandlerExceptionResolver.class);
    jwtService = mock(JwtService.class);
    customUserDetailsService = mock(CustomUserDetailsService.class);
    // JwtAuthFilter is real, but we need to set services it uses to our mocks.
    jwtAuthFilter = new JwtAuthFilter(jwtService, customUserDetailsService, handlerExceptionResolver);
  }

  //

  @Test
  void validJwt() throws Exception {
    // Arrange: Setup mock HTTP request & response. We will pretend TOKEN_VALID is, in fact, valid.
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Bearer valid.jwt.token");
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain filterChain = new MockFilterChain();

    // Arrange: Mock services.
    String email = "testuser@example.com";
    CustomUserDetails customUserDetails = new CustomUserDetails(1L, true, false, "Jan Kowalski", email, "p@S5wordN1c3", Set.of(TOKEN_VALID), null);

    when(jwtService.extractEmail(TOKEN_VALID)).thenReturn(email);
    when(jwtService.isTokenValid(TOKEN_VALID, customUserDetails.getEmail())).thenReturn(true);
    when(customUserDetailsService.loadUserByUsername(email)).thenReturn(customUserDetails);

    // Act: Execute the filter.
    jwtAuthFilter.doFilterInternal(request, response, filterChain);

    // Assert: Security Context is populated with the right principal.
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    assertThat(authentication).as("Authentication data should exist").isNotNull();
    assertThat(authentication.isAuthenticated()).as("User should be authenticated").isTrue();
    assertThat(authentication.getPrincipal()).as("Principal should be instance of CustomUserDetails").isInstanceOf(CustomUserDetails.class);

    // Assert the principal is our CustomUserDetails.
    CustomUserDetails principal = (CustomUserDetails) authentication.getPrincipal();
    assertThat(principal).isNotNull();
    assertThat(principal.getId()).isEqualTo(1L);
    assertThat(principal.getActive()).isTrue();
    assertThat(principal.getLocked()).isFalse();
    assertThat(principal.getUsername()).isEqualTo("Jan Kowalski");
    assertThat(principal.getEmail()).isEqualTo(email);
    assertThat(principal.getPassword()).isEqualTo("p@S5wordN1c3");
    assertThat(principal.getJwts()).isEqualTo(Set.of(TOKEN_VALID));
    assertThat(principal.getAuthorities()).isEqualTo(List.of());

    verify(jwtService).extractEmail(TOKEN_VALID);
    verify(customUserDetailsService).loadUserByUsername(email);
    verify(jwtService).isTokenValid(TOKEN_VALID, email);
    verifyNoInteractions(handlerExceptionResolver);
  }

  //

  @Test
  void missingHeader() throws Exception {
    // Arrange
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain filterChain = new MockFilterChain();

    // Act: Execute the filter.
    jwtAuthFilter.doFilterInternal(request, response, filterChain);

    // Assert: No authentication performed.
    assertThat(SecurityContextHolder.getContext().getAuthentication())
        .as("Authentication should not be created when header is missing")
        .isNull();

    verifyNoInteractions(jwtService, customUserDetailsService, handlerExceptionResolver);
  }

  @Test
  void malformedToken() throws Exception {
    // Arrange
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Bearer " + TOKEN_BAD);
    MockHttpServletResponse response = new MockHttpServletResponse();
    MockFilterChain filterChain = new MockFilterChain();

    when(jwtService.extractEmail(TOKEN_BAD))
        .thenThrow(new RuntimeException("Malformed JWT"));

    // Act: Execute the filter.
    jwtAuthFilter.doFilterInternal(request, response, filterChain);

    // Assert: No authentication performed.
    assertThat(SecurityContextHolder.getContext().getAuthentication())
        .as("Authentication should not be created for malformed token")
        .isNull();

    verify(jwtService).extractEmail(TOKEN_BAD);
    verify(customUserDetailsService, never()).loadUserByUsername(any());
    verify(handlerExceptionResolver).resolveException(
        eq(request),
        eq(response),
        isNull(),
        any(InvalidBearerTokenException.class)
    );
  }
}
