package org.portfolio.userland.system.auth.jwt;

import com.google.common.collect.Maps;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.portfolio.userland.features.user.BaseUserTest;
import org.portfolio.userland.features.user.entities.EnUserStatus;
import org.portfolio.userland.features.user.entities.Permission;
import org.portfolio.userland.features.user.entities.User;
import org.portfolio.userland.features.user.exceptions.UserInvalidStatusException;
import org.portfolio.userland.features.user.exceptions.UserLockedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Test <code>JwtService</code>.
 */
public class JwtServiceTest extends BaseUserTest {
  /** JWT service. */
  @Autowired
  private JwtService jwtService;

  /** How long before JWT token expires in minutes. */
  @Value("${security.jwt.expiration}")
  private long jwtExpiration;

  @AfterEach
  public void tearDown() {
    resetDatabase();
  }

  // //////////////////////////////////////////////////////////////////////////

  @Test
  void generateValidToken() {
    clock.setFixedTime("2026-04-10T10:05:00Z");

    // Arrange: Create a real user and token for that user.
    User user = userRepository.save(userFactory.genRandUser(EnUserStatus.ACTIVE));
    String token = jwtService.generateToken(user); // default expiration

    // Act: Check if token is valid.
    boolean isValid = jwtService.isTokenValid(token, user.getEmail());

    // prepare data
    long iat = 1775815500L; // fixed time
    long exp = iat + jwtExpiration*60L; // default expiration period

    // Assert: Validity and claims of token.
    assertThat(isValid).as("Token must be valid").isTrue();
    Map<String, Object> actualClaimMap = jwtService.extractAllClaims(token);
    Map<String, Object> expectedClaimMap = Maps.newHashMap();
    expectedClaimMap.put("iat", iat); // issued
    expectedClaimMap.put("exp", exp); // expires
    expectedClaimMap.put("sub", user.getEmail()); // user account email as subject
    expectedClaimMap.put("name", user.getUsername()); // username
    assertThat(actualClaimMap).as("Claim map is invalid").isEqualTo(expectedClaimMap);
  }

  @Test
  void generateValidTokenWithCustomExpiration() {
    clock.setFixedTime("2026-04-10T10:05:00Z");

    // Arrange: Create a real user and token for that user.
    User user = userRepository.save(userFactory.genRandUser(EnUserStatus.ACTIVE));
    String token = jwtService.generateToken(user, 60L); // only one hour

    // Act: Check if token is valid.
    boolean isValid = jwtService.isTokenValid(token, user.getEmail());

    // prepare data
    long iat = 1775815500L; // fixed time
    long exp = iat + 3600L; // 60 minutes later as specified in custom expiration

    // Assert: Validity and claims of token.
    assertThat(isValid).as("Token must be valid").isTrue();
    Map<String, Object> actualClaimMap = jwtService.extractAllClaims(token);
    Map<String, Object> expectedClaimMap = Maps.newHashMap();
    expectedClaimMap.put("iat", iat); // issued
    expectedClaimMap.put("exp", exp); // expires in hour
    expectedClaimMap.put("sub", user.getEmail()); // user account email as subject
    expectedClaimMap.put("name", user.getUsername()); // username
    assertThat(actualClaimMap).as("Claim map is invalid").isEqualTo(expectedClaimMap);
  }

  @Test
  void generateValidTokenWithPermissions() {
    clock.setFixedTime("2026-04-10T10:05:00Z");

    // Arrange: Create a real user and token for that user. User has a bunch of permissions.
    Permission permRole = permissionRepository.findByName("role").orElseThrow();
    Permission permUser = permissionRepository.findByName("user").orElseThrow();
    User user = userRepository.save(userFactory.genRandUser(EnUserStatus.ACTIVE));
    userPermissionFactory.genPermissionEntry(user, permUser, "edit");
    userPermissionFactory.genPermissionEntry(user, permRole, "admin"); // note two perms for same role
    userPermissionFactory.genPermissionEntry(user, permRole, "operator");
    String token = jwtService.generateToken(user); // default expiration

    // Act: Check if token is valid.
    boolean isValid = jwtService.isTokenValid(token, user.getEmail());

    // prepare data
    long iat = 1775815500L; // fixed time
    long exp = iat + jwtExpiration*60L; // default expiration period

    // Assert: Validity and claims of token.
    assertThat(isValid).as("Token must be valid").isTrue();
    Map<String, Object> actualClaimMap = jwtService.extractAllClaims(token);
    Map<String, Object> expectedClaimMap = Maps.newHashMap();
    expectedClaimMap.put("iat", iat); // issued
    expectedClaimMap.put("exp", exp); // expires
    expectedClaimMap.put("sub", user.getEmail()); // user account email as subject
    expectedClaimMap.put("name", user.getUsername()); // username
    expectedClaimMap.put("user", "edit");
    expectedClaimMap.put("role", "admin,operator"); // two perms for same role handled properly
    assertThat(actualClaimMap).as("Claim map is invalid").isEqualTo(expectedClaimMap);
  }

  //

  @Test
  void errPendingUser() {
    clock.setFixedTime("2026-04-10T10:00:00Z");

    // Arrange: Create a real user that is pending.
    final User user = userFactory.genRandUser(EnUserStatus.PENDING);

    // Act & Assert: Try to create a real token.
    assertThrows(
        UserInvalidStatusException.class,
        () -> jwtService.generateToken(user)
    );
  }

  @Test
  void errLockedUser() {
    clock.setFixedTime("2026-04-10T10:00:00Z");

    // Arrange: Create a real user that is locked.
    User tempUser = userFactory.genRandUser(EnUserStatus.ACTIVE);
    tempUser.setLocked(true);
    final User user = userRepository.save(tempUser);

    // Act & Assert: Try to create a real token.
    assertThrows(
        UserLockedException.class,
        () -> jwtService.generateToken(user)
    );
  }
}
