package org.portfolio.userland.common.services.jwt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.portfolio.userland.common.services.security.UserLandDetails;
import org.portfolio.userland.common.services.security.UserLandDetailsService;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests <code>JwtAuthFilter</code> in isolation.
 */
public class JwtAuthFilterTest {
  private final static String TOKEN_VALID = "valid.jwt.token";

  private JwtAuthFilter jwtAuthFilter;
  private JwtService jwtService;
  private UserLandDetailsService userLandDetailsService;

  @BeforeEach
  void setUp() {
    // Clear context amd reset mocks before each test.
    SecurityContextHolder.clearContext();

    jwtService = mock(JwtService.class);
    userLandDetailsService = mock(UserLandDetailsService.class);
    // JwtAuthFilter is real, but we need to set services it uses to our mocks.
    jwtAuthFilter = new JwtAuthFilter(jwtService, userLandDetailsService);
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
    UserLandDetails userLandDetails = new UserLandDetails(1L, true, false, "Jan Kowalski", email, "p@S5wordN1c3", Set.of(TOKEN_VALID), null);

    when(jwtService.extractEmail(TOKEN_VALID)).thenReturn(email);
    when(jwtService.isTokenValid(TOKEN_VALID, userLandDetails.getEmail())).thenReturn(true);
    when(userLandDetailsService.loadUserByUsername(email)).thenReturn(userLandDetails);

    // Act: Execute the filter.
    jwtAuthFilter.doFilterInternal(request, response, filterChain);

    // Assert: Security Context is populated with the right principal.
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    assertThat(authentication).as("Authentication data should exist").isNotNull();
    assertThat(authentication.isAuthenticated()).as("User should be authenticated").isTrue();

    // Assert the principal is our UserLandDetails.
    UserLandDetails principal = (UserLandDetails) authentication.getPrincipal();
    assertThat(principal).isNotNull();
    assertThat(principal.getId()).isEqualTo(1L);
    assertThat(principal.getActive()).isTrue();
    assertThat(principal.getLocked()).isFalse();
    assertThat(principal.getUsername()).isEqualTo("Jan Kowalski");
    assertThat(principal.getEmail()).isEqualTo(email);
    assertThat(principal.getPassword()).isEqualTo("p@S5wordN1c3");
    assertThat(principal.getJwts()).isEqualTo(Set.of(TOKEN_VALID));
    assertThat(principal.getAuthorities()).isEqualTo(List.of());
  }
}
