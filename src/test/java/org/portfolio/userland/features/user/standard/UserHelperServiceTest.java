package org.portfolio.userland.features.user.standard;

import org.junit.jupiter.api.Test;
import org.portfolio.userland.features.user.BaseUserTest;
import org.portfolio.userland.features.user.entities.EnUserStatus;
import org.portfolio.userland.features.user.entities.EnUserTokenType;
import org.portfolio.userland.features.user.entities.User;
import org.portfolio.userland.features.user.exceptions.UserInvalidStatusException;
import org.portfolio.userland.features.user.exceptions.UserLockedException;
import org.portfolio.userland.features.user.exceptions.UserNotFoundException;
import org.portfolio.userland.features.user.exceptions.UserWrongPasswordException;
import org.portfolio.userland.features.user.services.standard.UserHelperService;
import org.portfolio.userland.test.helpers.context.WithMockCustomUser;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Integration test for {@link UserHelperService}.
 */
public class UserHelperServiceTest extends BaseUserTest {
  @Autowired
  private UserHelperService userHelperService;

  // //////////////////////////////////////////////////////////////////////////
  // verifyUser

  @Test
  public void verifyUserActiveAndNotLocked() {
    // Arrange
    User user = userFactory.genUser(EnUserStatus.ACTIVE);

    // Act
    boolean result = userHelperService.verifyUser(user, true);

    // Assert
    assertThat(result).as("Active, unlocked user should pass verification").isTrue();
  }

  @Test
  public void verifyUserInvalidStatusThrows() {
    // Arrange
    User user = userFactory.genUser(EnUserStatus.PENDING);

    // Act & Assert
    assertThatThrownBy(() -> userHelperService.verifyUser(user, false))
        .as("Non-active user should throw UserInvalidStatusException")
        .isInstanceOf(UserInvalidStatusException.class);
  }

  @Test
  public void verifyUserLockedThrows() {
    // Arrange
    User user = userFactory.genUser(EnUserStatus.ACTIVE);
    user.setLocked(true);

    // Act & Assert
    assertThatThrownBy(() -> userHelperService.verifyUser(user, false))
        .as("Locked user should throw UserLockedException")
        .isInstanceOf(UserLockedException.class);
  }

  @Test
  public void verifyUserInvalidStatusSilent() {
    // Arrange
    User user = userFactory.genUser(EnUserStatus.PENDING);

    // Act
    boolean result = userHelperService.verifyUser(user, true);

    // Assert
    assertThat(result).as("Non-active user should return false when failSilently").isFalse();
  }

  @Test
  public void verifyUserLockedSilent() {
    // Arrange
    User user = userFactory.genUser(EnUserStatus.ACTIVE);
    user.setLocked(true);

    // Act
    boolean result = userHelperService.verifyUser(user, true);

    // Assert
    assertThat(result).as("Locked user should return false when failSilently").isFalse();
  }

  // //////////////////////////////////////////////////////////////////////////
  // verifyPassword

  @Test
  public void verifyPasswordCorrect() {
    // Arrange
    User user = userFactory.genUser(EnUserStatus.ACTIVE);

    // Act & Assert (no exception thrown)
    userHelperService.verifyPassword(user, "Password123!");
  }

  @Test
  public void verifyPasswordIncorrect() {
    // Arrange
    User user = userFactory.genUser(EnUserStatus.ACTIVE);

    // Act & Assert
    assertThatThrownBy(() -> userHelperService.verifyPassword(user, "wrongPassword"))
        .as("Wrong password should throw UserWrongPasswordException")
        .isInstanceOf(UserWrongPasswordException.class);
  }

  // //////////////////////////////////////////////////////////////////////////
  // resolveUser(String email, boolean failSilently)

  @Test
  public void resolveByEmailFound() {
    // Arrange
    User expectedUser = userFactory.genUser(EnUserStatus.ACTIVE);
    userRepository.save(expectedUser);

    // Act
    User actualUser = userHelperService.resolveUser("test@example.com", false);

    // Assert
    assertThat(actualUser).as("Should find active user by email").isNotNull();
    assertThat(actualUser.getEmail()).as("Email should match").isEqualTo("test@example.com");
  }

  @Test
  public void resolveByEmailNotFoundThrows() {
    // Act & Assert
    assertThatThrownBy(() -> userHelperService.resolveUser("nonexistent@test.com", false))
        .as("Non-existent email should throw UserNotFoundException")
        .isInstanceOf(UserNotFoundException.class);
  }

  @Test
  public void resolveByEmailNotFoundSilent() {
    // Act
    User result = userHelperService.resolveUser("nonexistent@test.com", true);

    // Assert
    assertThat(result).as("Non-existent email should return null when failSilently").isNull();
  }

  @Test
  public void resolveByEmailInvalidStatusThrows() {
    // Arrange
    User expectedUser = userFactory.genUser(EnUserStatus.PENDING);
    userRepository.save(expectedUser);

    // Act & Assert
    assertThatThrownBy(() -> userHelperService.resolveUser("test@example.com", false))
        .as("Pending user should throw UserInvalidStatusException")
        .isInstanceOf(UserInvalidStatusException.class);
  }

  // //////////////////////////////////////////////////////////////////////////
  // resolveUser(Long id, boolean failSilently, boolean verify)

  @Test
  public void resolveByIdFound() {
    // Arrange
    User expectedUser = userFactory.genUser(EnUserStatus.ACTIVE);
    userRepository.save(expectedUser);

    // Act
    User actualUser = userHelperService.resolveUser(expectedUser.getId(), false, true);

    // Assert
    assertThat(actualUser).as("Should find active user by id").isNotNull();
    assertThat(actualUser.getId()).as("Id should match").isEqualTo(expectedUser.getId());
  }

  @Test
  public void resolveByIdNotFoundThrows() {
    // Act & Assert
    assertThatThrownBy(() -> userHelperService.resolveUser(999999L, false, true))
        .as("Non-existent id should throw UserNotFoundException")
        .isInstanceOf(UserNotFoundException.class);
  }

  @Test
  public void resolveByIdNotFoundSilent() {
    // Act
    User result = userHelperService.resolveUser(999999L, true, true);

    // Assert
    assertThat(result).as("Non-existent id should return null when failSilently").isNull();
  }

  @Test
  public void resolveByIdSkipVerification() {
    // Arrange
    User expectedUser = userFactory.genUser(EnUserStatus.PENDING);
    userRepository.save(expectedUser);

    // Act
    User actualUser = userHelperService.resolveUser(expectedUser.getId(), false, false);

    // Assert
    assertThat(actualUser).as("Should return pending user when verify=false").isNotNull();
    assertThat(actualUser.getStatus()).as("Status should be PENDING").isEqualTo(EnUserStatus.PENDING);
  }

  // //////////////////////////////////////////////////////////////////////////
  // resolveAuthUser(Long id, boolean failSilently)

  @Test
  public void resolveAuthByIdFound() {
    // Arrange
    User expectedUser = userFactory.genUser(EnUserStatus.ACTIVE);
    userRepository.save(expectedUser);

    // Act
    User actualUser = userHelperService.resolveAuthUser(expectedUser.getId(), false);

    // Assert
    assertThat(actualUser).as("Should find active user via auth lookup by id").isNotNull();
    assertThat(actualUser.getId()).as("Id should match").isEqualTo(expectedUser.getId());
  }

  @Test
  public void resolveAuthByIdNotFoundThrows() {
    // Act & Assert
    assertThatThrownBy(() -> userHelperService.resolveAuthUser(999999L, false))
        .as("Non-existent id should throw UserNotFoundException")
        .isInstanceOf(UserNotFoundException.class);
  }

  @Test
  public void resolveAuthByIdInvalidStatusThrows() {
    // Arrange
    User expectedUser = userFactory.genUser(EnUserStatus.PENDING);
    userRepository.save(expectedUser);

    // Act & Assert
    assertThatThrownBy(() -> userHelperService.resolveAuthUser(expectedUser.getId(), false))
        .as("Pending user should throw UserInvalidStatusException")
        .isInstanceOf(UserInvalidStatusException.class);
  }

  // //////////////////////////////////////////////////////////////////////////
  // resolveAuthUser(String email, boolean failSilently)

  @Test
  public void resolveAuthByEmailFound() {
    // Arrange
    User expectedUser = userFactory.genUser(EnUserStatus.ACTIVE);
    userRepository.save(expectedUser);

    // Act
    User actualUser = userHelperService.resolveAuthUser("test@example.com", false);

    // Assert
    assertThat(actualUser).as("Should find active user via auth lookup by email").isNotNull();
    assertThat(actualUser.getEmail()).as("Email should match").isEqualTo("test@example.com");
  }

  @Test
  public void resolveAuthByEmailNotFoundThrows() {
    // Act & Assert
    assertThatThrownBy(() -> userHelperService.resolveAuthUser("nonexistent@test.com", false))
        .as("Non-existent email should throw UserNotFoundException")
        .isInstanceOf(UserNotFoundException.class);
  }

  @Test
  public void resolveAuthByEmailInvalidStatusThrows() {
    // Arrange
    User expectedUser = userFactory.genUser(EnUserStatus.PENDING);
    userRepository.save(expectedUser);

    // Act & Assert
    assertThatThrownBy(() -> userHelperService.resolveAuthUser("test@example.com", false))
        .as("Pending user should throw UserInvalidStatusException")
        .isInstanceOf(UserInvalidStatusException.class);
  }

  // //////////////////////////////////////////////////////////////////////////
  // resolveExpirationTime / resolveExpirationDuration / resolveExpirationSince

  @Test
  public void resolveExpirationTimeActivation() {
    // Act
    long minutes = userHelperService.resolveExpirationTime(EnUserTokenType.ACTIVATE);

    // Assert
    assertThat(minutes).as("Activation token expiration should be 1440 minutes (24h)").isEqualTo(1440);
  }

  @Test
  public void resolveExpirationTimeEmail() {
    // Act
    long minutes = userHelperService.resolveExpirationTime(EnUserTokenType.EMAIL);

    // Assert
    assertThat(minutes).as("Email token expiration should be 30 minutes").isEqualTo(30);
  }

  @Test
  public void resolveExpirationTimePassword() {
    // Act
    long minutes = userHelperService.resolveExpirationTime(EnUserTokenType.PASSWORD);

    // Assert
    assertThat(minutes).as("Password token expiration should be 30 minutes").isEqualTo(30);
  }

  @Test
  public void resolveExpirationTimeDeletion() {
    // Act
    long minutes = userHelperService.resolveExpirationTime(EnUserTokenType.DELETE);

    // Assert
    assertThat(minutes).as("Deletion token expiration should be 30 minutes").isEqualTo(30);
  }

  @Test
  public void resolveExpirationDuration() {
    // Act
    Duration duration = userHelperService.resolveExpirationDuration(EnUserTokenType.EMAIL);

    // Assert
    assertThat(duration).as("Duration should match token expiration").isEqualTo(Duration.ofMinutes(30));
  }

  @Test
  public void resolveExpirationSince() {
    // Arrange
    LocalDateTime nowAt = LocalDateTime.of(2026, 4, 10, 10, 0, 0);

    // Act
    LocalDateTime result = userHelperService.resolveExpirationSince(nowAt, EnUserTokenType.EMAIL);

    // Assert
    assertThat(result).as("Expiration should be now + 30 minutes")
        .isEqualTo(LocalDateTime.of(2026, 4, 10, 10, 30, 0));
  }

  // //////////////////////////////////////////////////////////////////////////
  // resolveJwtExpiration

  @Test
  public void resolveJwtExpirationDefault() {
    // Arrange
    LocalDateTime issuedAt = LocalDateTime.of(2026, 4, 10, 10, 0, 0);

    // Act
    LocalDateTime result = userHelperService.resolveJwtExpiration(issuedAt, null);

    // Assert
    assertThat(result).as("Default JWT expiration should be issuedAt + 360 min (6h)")
        .isEqualTo(LocalDateTime.of(2026, 4, 10, 16, 0, 0));
  }

  @Test
  public void resolveJwtExpirationCustom() {
    // Arrange
    LocalDateTime issuedAt = LocalDateTime.of(2026, 4, 10, 10, 0, 0);

    // Act
    LocalDateTime result = userHelperService.resolveJwtExpiration(issuedAt, 60L);

    // Assert
    assertThat(result).as("Custom JWT expiration should be issuedAt + 60 min")
        .isEqualTo(LocalDateTime.of(2026, 4, 10, 11, 0, 0));
  }

  @Test
  public void resolveJwtExpirationClampMin() {
    // Arrange
    LocalDateTime issuedAt = LocalDateTime.of(2026, 4, 10, 10, 0, 0);

    // Act
    LocalDateTime result = userHelperService.resolveJwtExpiration(issuedAt, 0L);

    // Assert
    assertThat(result).as("JWT expiration should clamp to MIN_EXPIRATION (1 min)")
        .isEqualTo(LocalDateTime.of(2026, 4, 10, 10, 1, 0));
  }

  @Test
  public void resolveJwtExpirationClampMax() {
    // Arrange
    LocalDateTime issuedAt = LocalDateTime.of(2026, 4, 10, 10, 0, 0);
    long maxMinutes = 60L * 24 * 31; // 44640

    // Act
    LocalDateTime result = userHelperService.resolveJwtExpiration(issuedAt, 999999L);

    // Assert
    assertThat(result).as("JWT expiration should clamp to MAX_EXPIRATION (31 days)")
        .isEqualTo(issuedAt.plusMinutes(maxMinutes));
  }

  // //////////////////////////////////////////////////////////////////////////
  // resolveUser(boolean failSilently) - from security context

  @Test
  @WithMockCustomUser(email = "test@example.com")
  public void resolveFromSecurityContext() {
    // Arrange
    User expectedUser = userFactory.genUser(EnUserStatus.ACTIVE);
    userRepository.save(expectedUser);

    // Act
    User actualUser = userHelperService.resolveUser(false);

    // Assert
    assertThat(actualUser).as("Should resolve user from security context").isNotNull();
    assertThat(actualUser.getEmail()).as("Email should match").isEqualTo("test@example.com");
  }

  @Test
  @WithMockCustomUser(email = "test@example.com")
  public void resolveFromSecurityContextNotFoundSilent() {
    // Act
    User result = userHelperService.resolveUser(true);

    // Assert
    assertThat(result).as("Should return null when user not in DB and failSilently").isNull();
  }
}
