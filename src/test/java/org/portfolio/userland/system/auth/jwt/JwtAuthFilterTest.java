package org.portfolio.userland.system.auth.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.MalformedJwtException;
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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.util.Arrays;
import java.util.List;
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

  private JwtService jwtService;
  private CustomUserDetailsService customUserDetailsService;
  private HandlerExceptionResolver handlerExceptionResolver;

  private JwtAuthFilter jwtAuthFilter;

  private RequestMatcher publicEndpointsMatcher;

  @BeforeEach
  void setup() {
    // Clear context and reset mocks before each test.
    SecurityContextHolder.clearContext();

    jwtService = mock(JwtService.class);
    customUserDetailsService = mock(CustomUserDetailsService.class);
    handlerExceptionResolver = mock(HandlerExceptionResolver.class);

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

    // Arrange: Mock services. Note claims are verified during parsing (signature & expiration),
    // authorities come from signed token claims.
    String email = "testuser@example.com";
    Claims claims = mock(Claims.class);
    when(claims.getSubject()).thenReturn(email);
    CustomUserDetails customUserDetails = new CustomUserDetails(
        1L, true, false, "Jan Kowalski", email,
        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"), new SimpleGrantedAuthority("USER_VIEW")));

    when(jwtService.extractAllClaims(TOKEN_VALID)).thenReturn(claims);
    when(customUserDetailsService.loadFromToken(claims, TOKEN_VALID)).thenReturn(customUserDetails);

    // Act: Execute the filter.
    jwtAuthFilter.doFilterInternal(request, response, filterChain);

    // Assert: Security Context is populated with the right principal.
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    assertThat(authentication).as("Authentication data should exist").isNotNull();
    assertThat(authentication.isAuthenticated()).as("User should be authenticated").isTrue();
    assertThat(authentication.getPrincipal()).as("Principal should be instance of CustomUserDetails").isInstanceOf(CustomUserDetails.class);

    // Assert: Principal is our CustomUserDetails with authorities resolved from token claims.
    CustomUserDetails principal = (CustomUserDetails) authentication.getPrincipal();
    assertThat(principal).isNotNull();
    assertThat(principal.getId()).isEqualTo(1L);
    assertThat(principal.isActive()).isTrue();
    assertThat(principal.isLocked()).isFalse();
    assertThat(principal.getUsername()).isEqualTo("Jan Kowalski");
    assertThat(principal.getEmail()).isEqualTo(email);
    assertThat(principal.getAuthorities()).isEqualTo(customUserDetails.getAuthorities());

    verify(jwtService).extractAllClaims(TOKEN_VALID);
    verify(customUserDetailsService).loadFromToken(claims, TOKEN_VALID);
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
    // Check behavior when we supply bad token.

    // Arrange: setup everything needed for filter execution.
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRequestURI("/api/protected/resource"); // Use a protected endpoint.
    request.addHeader("Authorization", "Bearer " + TOKEN_BAD);
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain filterChain = mock(FilterChain.class);

    // Note: filter catches only JwtException/IllegalArgumentException - stub with the real exception type
    // parseSignedClaims() throws for malformed tokens. A RuntimeException would propagate as a genuine bug.
    when(jwtService.extractAllClaims(TOKEN_BAD)).thenThrow(new MalformedJwtException("Malformed JWT"));

    // Act: Execute the filter.
    jwtAuthFilter.doFilterInternal(request, response, filterChain);

    // Assert: No authentication performed.
    assertThat(SecurityContextHolder.getContext().getAuthentication())
        .as("Authentication should not be created for malformed token on protected endpoint")
        .isNull();

    // Assert: Exception happened.
    verify(jwtService).extractAllClaims(TOKEN_BAD);
    verify(customUserDetailsService, never()).loadFromToken(any(), any());
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
    // Public endpoint should work with malformed token - token is just ignored, as if you are not logged.

    // Arrange: setup everything needed for filter execution.
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRequestURI(EndpointConst.PUBLIC[0]); // Use a public endpoint.
    request.addHeader("Authorization", "Bearer " + TOKEN_BAD);
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain filterChain = mock(FilterChain.class);

    when(jwtService.extractAllClaims(TOKEN_BAD)).thenThrow(new MalformedJwtException("Malformed JWT"));

    // Act: Execute the filter.
    jwtAuthFilter.doFilterInternal(request, response, filterChain);

    // Assert: No authentication performed.
    assertThat(SecurityContextHolder.getContext().getAuthentication())
        .as("Authentication should not be created for malformed token on public endpoint")
        .isNull();

    verify(jwtService).extractAllClaims(TOKEN_BAD);
    verify(customUserDetailsService, never()).loadFromToken(any(), any());
    // Assert: The handlerExceptionResolver should NOT be called for public endpoints with malformed tokens
    verifyNoInteractions(handlerExceptionResolver);
    // Assert: The filter chain should continue.
    verify(filterChain, times(1)).doFilter(request, response);
  }

  //

  @Test
  void revokedTokenIsRejected() throws Exception {
    // Arrange: Token parses fine, but its entry was deleted from database (logout/revocation). Revocation is
    // checked inside the service via the database join, which then returns no user details.

    // Arrange: Setup state where token is good, but was revoked.
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRequestURI("/api/protected/resource");
    request.addHeader("Authorization", "Bearer " + TOKEN_VALID);
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain filterChain = mock(FilterChain.class);

    String email = "testuser@example.com";
    Claims claims = mock(Claims.class);
    when(claims.getSubject()).thenReturn(email);

    when(jwtService.extractAllClaims(TOKEN_VALID)).thenReturn(claims);
    when(customUserDetailsService.loadFromToken(claims, TOKEN_VALID)).thenReturn(null);

    // Act: Execute the filter.
    jwtAuthFilter.doFilterInternal(request, response, filterChain);

    // Assert: No authentication created - revocation check must win over valid signature.
    assertThat(SecurityContextHolder.getContext().getAuthentication())
        .as("Authentication should not be created for revoked token")
        .isNull();
    verify(filterChain, times(1)).doFilter(request, response);
  }

  @Test
  void unknownUserIsRejected() throws Exception {
    // Token parses fine, but user does not exist anymore. Same mechanism as revocation - service returns
    // no user details when the email/token pair has no row in database.

    // Arrange: Setup state where token is good, but user is gone.
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setRequestURI("/api/protected/resource");
    request.addHeader("Authorization", "Bearer " + TOKEN_VALID);
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain filterChain = mock(FilterChain.class);

    Claims claims = mock(Claims.class);
    when(jwtService.extractAllClaims(TOKEN_VALID)).thenReturn(claims);
    when(customUserDetailsService.loadFromToken(claims, TOKEN_VALID)).thenReturn(null);

    // Act: Execute the filter.
    jwtAuthFilter.doFilterInternal(request, response, filterChain);

    // Assert: No authentication created.
    assertThat(SecurityContextHolder.getContext().getAuthentication())
        .as("Authentication should not be created for non-existing user")
        .isNull();
    verify(filterChain, times(1)).doFilter(request, response);
  }
}
