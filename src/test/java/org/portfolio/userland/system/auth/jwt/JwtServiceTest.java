package org.portfolio.userland.system.auth.jwt;

import com.google.common.collect.Maps;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.portfolio.userland.features.user.BaseUserTest;
import org.portfolio.userland.features.user.entities.EnUserStatus;
import org.portfolio.userland.features.user.entities.User;
import org.portfolio.userland.features.user.exceptions.UserInvalidStatusException;
import org.portfolio.userland.features.user.exceptions.UserLockedException;
import org.springframework.beans.factory.annotation.Autowired;

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
    String token = jwtService.generateToken(user);

    // Act: Check if token is valid.
    boolean isValid = jwtService.isTokenValid(token, user.getEmail());

    // Assert: Validity and claims of token.
    assertThat(isValid).as("Token must be valid").isTrue();
    Map<String, Object> actualClaimMap = jwtService.extractAllClaims(token);
    Map<String, Object> expectedClaimMap = Maps.newHashMap();
    expectedClaimMap.put("iat", 1775815500L); // issued
    expectedClaimMap.put("exp", 1775837100L); // expires
    expectedClaimMap.put("sub", user.getEmail()); // user account email as subject
    assertThat(actualClaimMap).as("Claim map is invalid").isEqualTo(expectedClaimMap);
  }

  //

  @Test
  void errPendingUser() {
    clock.setFixedTime("2026-04-10T10:00:00Z");

    // Arrange: Create a real user that is pending.
    final User user = userFactory.genRandUser(EnUserStatus.PENDING);

    // Act & Assert: Try to create a real token.
    UserInvalidStatusException actualEx = assertThrows(
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
