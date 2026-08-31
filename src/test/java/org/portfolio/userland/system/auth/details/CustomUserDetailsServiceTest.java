package org.portfolio.userland.system.auth.details;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.portfolio.userland.features.user.entities.EnUserStatus;
import org.portfolio.userland.features.user.repositories.user.UserAuthState;
import org.portfolio.userland.features.user.repositories.user.UserRepository;
import org.portfolio.userland.system.auth.jwt.constants.JwtClaims;
import org.portfolio.userland.test.base.AnyTest;
import org.springframework.security.core.GrantedAuthority;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Tests <code>CustomUserDetailsService</code> in isolation.
 */
@AnyTest
class CustomUserDetailsServiceTest {
  private static final String EMAIL = "testuser@example.com";
  private static final String JWT_STRING = "valid.jwt.token";

  private UserRepository userRepository;
  private CustomUserDetailsService service;

  @BeforeEach
  void setUp() {
    userRepository = mock(UserRepository.class);
    service = new CustomUserDetailsService(userRepository);
  }

  //

  @Test
  void loadFromToken_userFound_active_notLocked_withNameClaim() {
    // Arrange: ACTIVE user, not locked, JWT has name claim.
    Claims claims = mock(Claims.class);
    when(claims.getSubject()).thenReturn(EMAIL);
    when(claims.get(JwtClaims.NAME, String.class)).thenReturn("Jan Kowalski");
    when(claims.get(JwtClaims.PERMS)).thenReturn(Map.of("role", "admin"));

    UserAuthState state = new UserAuthState(1L, EnUserStatus.ACTIVE, false);
    when(userRepository.findAuthStateByEmailAndToken(EMAIL, JWT_STRING)).thenReturn(Optional.of(state));

    // Act
    CustomUserDetails result = service.loadFromToken(claims, JWT_STRING);

    // Assert
    assertThat(result).as("Result should not be null for existing user").isNotNull();
    assertThat(result.getId()).as("User ID should match").isEqualTo(1L);
    assertThat(result.isActive()).as("Active user should have active=true").isTrue();
    assertThat(result.isLocked()).as("Unlocked user should have locked=false").isFalse();
    assertThat(result.getUsername()).as("Username should come from name claim").isEqualTo("Jan Kowalski");
    assertThat(result.getEmail()).as("Email should match subject").isEqualTo(EMAIL);
    List<String> authorityStrings = result.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .toList();
    assertThat(authorityStrings)
        .as("Authorities should be resolved from perms claim")
        .containsExactly("ROLE_ADMIN");

    verify(userRepository).findAuthStateByEmailAndToken(EMAIL, JWT_STRING);
  }

  @Test
  void loadFromToken_userFound_active_notLocked_nameClaimMissing() {
    // Arrange: ACTIVE user, not locked, JWT has no name claim — should fall back to subject (email).
    Claims claims = mock(Claims.class);
    when(claims.getSubject()).thenReturn(EMAIL);
    when(claims.get(JwtClaims.NAME, String.class)).thenReturn(null);
    when(claims.get(JwtClaims.PERMS)).thenReturn(null);

    UserAuthState state = new UserAuthState(1L, EnUserStatus.ACTIVE, false);
    when(userRepository.findAuthStateByEmailAndToken(EMAIL, JWT_STRING)).thenReturn(Optional.of(state));

    // Act
    CustomUserDetails result = service.loadFromToken(claims, JWT_STRING);

    // Assert
    assertThat(result).as("Result should not be null").isNotNull();
    assertThat(result.getUsername()).as("Username should fall back to email when name claim is missing").isEqualTo(EMAIL);
  }

  @Test
  void loadFromToken_userNotFound_returnsNull() {
    // Arrange: No user/token pair in database (unknown user or revoked token).
    Claims claims = mock(Claims.class);
    when(claims.getSubject()).thenReturn(EMAIL);
    when(userRepository.findAuthStateByEmailAndToken(EMAIL, JWT_STRING)).thenReturn(Optional.empty());

    // Act
    CustomUserDetails result = service.loadFromToken(claims, JWT_STRING);

    // Assert
    assertThat(result).as("Result should be null for non-existing user/token").isNull();
  }

  @Test
  void loadFromToken_pendingUser_activeFalse() {
    // Arrange: PENDING user should result in active=false.
    Claims claims = mock(Claims.class);
    when(claims.getSubject()).thenReturn(EMAIL);
    when(claims.get(JwtClaims.NAME, String.class)).thenReturn("Jan Kowalski");
    when(claims.get(JwtClaims.PERMS)).thenReturn(null);

    UserAuthState state = new UserAuthState(1L, EnUserStatus.PENDING, false);
    when(userRepository.findAuthStateByEmailAndToken(EMAIL, JWT_STRING)).thenReturn(Optional.of(state));

    // Act
    CustomUserDetails result = service.loadFromToken(claims, JWT_STRING);

    // Assert
    assertThat(result).as("Result should not be null").isNotNull();
    assertThat(result.isActive()).as("PENDING user should have active=false").isFalse();
  }

  @Test
  void loadFromToken_demoUser_activeFalse() {
    // Arrange: DEMO user should result in active=false.
    Claims claims = mock(Claims.class);
    when(claims.getSubject()).thenReturn(EMAIL);
    when(claims.get(JwtClaims.NAME, String.class)).thenReturn("Jan Kowalski");
    when(claims.get(JwtClaims.PERMS)).thenReturn(null);

    UserAuthState state = new UserAuthState(1L, EnUserStatus.DEMO, false);
    when(userRepository.findAuthStateByEmailAndToken(EMAIL, JWT_STRING)).thenReturn(Optional.of(state));

    // Act
    CustomUserDetails result = service.loadFromToken(claims, JWT_STRING);

    // Assert
    assertThat(result).as("Result should not be null").isNotNull();
    assertThat(result.isActive()).as("DEMO user should have active=false").isFalse();
  }

  @Test
  void loadFromToken_lockedUser() {
    // Arrange: Locked user should result in locked=true.
    Claims claims = mock(Claims.class);
    when(claims.getSubject()).thenReturn(EMAIL);
    when(claims.get(JwtClaims.NAME, String.class)).thenReturn("Jan Kowalski");
    when(claims.get(JwtClaims.PERMS)).thenReturn(null);

    UserAuthState state = new UserAuthState(1L, EnUserStatus.ACTIVE, true);
    when(userRepository.findAuthStateByEmailAndToken(EMAIL, JWT_STRING)).thenReturn(Optional.of(state));

    // Act
    CustomUserDetails result = service.loadFromToken(claims, JWT_STRING);

    // Assert
    assertThat(result).as("Result should not be null").isNotNull();
    assertThat(result.isLocked()).as("Locked user should have locked=true").isTrue();
  }

  @Test
  void loadFromToken_nullLockedFromDb_treatedAsNotLocked() {
    // Arrange: locked=null from database should be treated as not locked (Boolean.TRUE.equals(null) = false).
    Claims claims = mock(Claims.class);
    when(claims.getSubject()).thenReturn(EMAIL);
    when(claims.get(JwtClaims.NAME, String.class)).thenReturn("Jan Kowalski");
    when(claims.get(JwtClaims.PERMS)).thenReturn(null);

    UserAuthState state = new UserAuthState(1L, EnUserStatus.ACTIVE, null);
    when(userRepository.findAuthStateByEmailAndToken(EMAIL, JWT_STRING)).thenReturn(Optional.of(state));

    // Act
    CustomUserDetails result = service.loadFromToken(claims, JWT_STRING);

    // Assert
    assertThat(result).as("Result should not be null").isNotNull();
    assertThat(result.isLocked()).as("null locked from DB should be treated as not locked").isFalse();
  }

  @Test
  void loadFromToken_withPermsClaim_resolvesAuthorities() {
    // Arrange: JWT has perms claim with multiple permissions.
    Claims claims = mock(Claims.class);
    when(claims.getSubject()).thenReturn(EMAIL);
    when(claims.get(JwtClaims.NAME, String.class)).thenReturn("Jan Kowalski");
    when(claims.get(JwtClaims.PERMS)).thenReturn(Map.of("role", "admin,operator", "user", "view"));

    UserAuthState state = new UserAuthState(1L, EnUserStatus.ACTIVE, false);
    when(userRepository.findAuthStateByEmailAndToken(EMAIL, JWT_STRING)).thenReturn(Optional.of(state));

    // Act
    CustomUserDetails result = service.loadFromToken(claims, JWT_STRING);

    // Assert
    assertThat(result).as("Result should not be null").isNotNull();
    List<String> authorityStrings = result.getAuthorities().stream()
        .map(GrantedAuthority::getAuthority)
        .toList();
    assertThat(authorityStrings)
        .as("Authorities should be resolved from perms claim")
        .containsExactlyInAnyOrder("ROLE_ADMIN", "ROLE_OPERATOR", "USER_VIEW");
    assertThat(authorityStrings).as("Authorities should be sorted").isSorted();
  }

  @Test
  void loadFromToken_noPermsClaim_emptyAuthorities() {
    // Arrange: JWT has no perms claim — authorities should be empty.
    Claims claims = mock(Claims.class);
    when(claims.getSubject()).thenReturn(EMAIL);
    when(claims.get(JwtClaims.NAME, String.class)).thenReturn("Jan Kowalski");
    when(claims.get(JwtClaims.PERMS)).thenReturn(null);

    UserAuthState state = new UserAuthState(1L, EnUserStatus.ACTIVE, false);
    when(userRepository.findAuthStateByEmailAndToken(EMAIL, JWT_STRING)).thenReturn(Optional.of(state));

    // Act
    CustomUserDetails result = service.loadFromToken(claims, JWT_STRING);

    // Assert
    assertThat(result).as("Result should not be null").isNotNull();
    assertThat(result.getAuthorities()).as("Authorities should be empty when perms claim is absent").isEmpty();
  }

  @Test
  void loadFromToken_nullPermsClaim_emptyAuthorities() {
    // Arrange: JWT has perms claim explicitly set to null — authorities should be empty.
    Claims claims = mock(Claims.class);
    when(claims.getSubject()).thenReturn(EMAIL);
    when(claims.get(JwtClaims.NAME, String.class)).thenReturn("Jan Kowalski");
    when(claims.get(JwtClaims.PERMS)).thenReturn(null);

    UserAuthState state = new UserAuthState(1L, EnUserStatus.ACTIVE, false);
    when(userRepository.findAuthStateByEmailAndToken(EMAIL, JWT_STRING)).thenReturn(Optional.of(state));

    // Act
    CustomUserDetails result = service.loadFromToken(claims, JWT_STRING);

    // Assert
    assertThat(result).as("Result should not be null").isNotNull();
    assertThat(result.getAuthorities()).as("Authorities should be empty when perms claim is null").isEmpty();
  }
}
