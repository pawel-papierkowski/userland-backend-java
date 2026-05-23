package org.portfolio.userland.system.auth.jwt;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.portfolio.userland.config.security.constants.EndpointConst;
import org.portfolio.userland.system.auth.details.CustomUserDetails;
import org.portfolio.userland.system.auth.details.CustomUserDetailsService;
import org.portfolio.userland.system.auth.jwt.exceptions.InvalidBearerTokenException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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

  private RequestMatcher publicEndpointsMatcher;

  @BeforeEach
  void setup() {
    // Clear context amd reset mocks before each test.
    SecurityContextHolder.clearContext();

    handlerExceptionResolver = mock(HandlerExceptionResolver.class);
    jwtService = mock(JwtService.class);
    customUserDetailsService = mock(CustomUserDetailsService.class);
    // JwtAuthFilter is real, but we need to set services it uses to our mocks.
    jwtAuthFilter = new JwtAuthFilter(jwtService, customUserDetailsService, handlerExceptionResolver);

    publicEndpointsMatcher = new OrRequestMatcher(
        Arrays.stream(EndpointConst.PUBLIC)
            .map(PathPatternRequestMatcher.withDefaults()::matcher)
            .collect(Collectors.toList())
    );
  }

  //

  @Test
  void validJwt() throws Exception {
    // Arrange: Setup mock HTTP request & response. We will pretend TOKEN_VALID is, in fact, valid.
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Bearer valid.jwt.token");
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain filterChain = mock(FilterChain.class);

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
    FilterChain filterChain = mock(FilterChain.class);

    // Act: Execute the filter.
    jwtAuthFilter.doFilterInternal(request, response, filterChain);

    // Assert: No authentication performed.
    assertThat(SecurityContextHolder.getContext().getAuthentication())
        .as("Authentication should not be created when header is missing")
        .isNull();

    verifyNoInteractions(jwtService, customUserDetailsService, handlerExceptionResolver);
  }

  @Test
  void malformedTokenOnProtectedEndpoint() throws Exception {
    // Arrange: setup everything needed for filter execution.
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRequestURI("/api/protected/resource"); // Use a protected endpoint.
    request.addHeader("Authorization", "Bearer " + TOKEN_BAD);
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain filterChain = mock(FilterChain.class);

    when(jwtService.extractEmail(TOKEN_BAD)).thenThrow(new RuntimeException("Malformed JWT"));

    // Act: Execute the filter.
    jwtAuthFilter.doFilterInternal(request, response, filterChain);

    // Assert: No authentication performed.
    assertThat(SecurityContextHolder.getContext().getAuthentication())
        .as("Authentication should not be created for malformed token on protected endpoint")
        .isNull();

    // Assert: Exception happened.
    verify(jwtService).extractEmail(TOKEN_BAD);
    verify(customUserDetailsService, never()).loadUserByUsername(any());
    verify(handlerExceptionResolver).resolveException(
        eq(request),
        eq(response),
        isNull(),
        any(InvalidBearerTokenException.class)
    );
    // Assert: Ensure filterChain.doFilter was not called after the exception was resolved.
    verify(filterChain, never()).doFilter(any(), any());
  }

  @Test
  void malformedTokenOnPublicEndpoint() throws Exception {
    // Arrange: setup everything needed for filter execution.
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRequestURI(EndpointConst.PUBLIC[0]); // Use a public endpoint.
    request.addHeader("Authorization", "Bearer " + TOKEN_BAD);
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain filterChain = mock(FilterChain.class);

    when(jwtService.extractEmail(TOKEN_BAD)).thenThrow(new RuntimeException("Malformed JWT"));

    // Act: Execute the filter.
    jwtAuthFilter.doFilterInternal(request, response, filterChain);

    // Assert: No authentication performed.
    assertThat(SecurityContextHolder.getContext().getAuthentication())
        .as("Authentication should not be created for malformed token on public endpoint")
        .isNull();

    verify(jwtService).extractEmail(TOKEN_BAD);
    verify(customUserDetailsService, never()).loadUserByUsername(any());
    // Assert: The handlerExceptionResolver should NOT be called for public endpoints with malformed tokens
    verifyNoInteractions(handlerExceptionResolver);
    // Assert: The filter chain should continue.
    verify(filterChain, times(1)).doFilter(request, response);
  }
}