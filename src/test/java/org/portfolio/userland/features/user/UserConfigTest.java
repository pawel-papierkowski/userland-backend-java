package org.portfolio.userland.features.user;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.portfolio.userland.features.user.entities.EnUserStatus;
import org.portfolio.userland.features.user.entities.User;
import org.portfolio.userland.features.user.services.UserConfigService;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test user configuration service.
 */
public class UserConfigTest extends BaseUserTest {
  @Autowired
  private UserConfigService userConfigService;

  @AfterEach
  public void tearDown() {
    resetDatabase();
  }

  // //////////////////////////////////////////////////////////////////////////

  @Test
  public void getMissingConfig() {
    clock.setFixedTime("2026-04-10T10:00:00Z");
    // Arrange: Create active user in database that has some config entry.
    User user = userFactory.genRandUser(EnUserStatus.ACTIVE);
    user.addConfig(userConfigFactory.genConfig(user, "test.variable", "1"));
    userRepository.save(user);

    // Act: get config that do not exist.
    String actualValue = userConfigService.get(user, "other.variable", "zz");

    // Assert: value is correct.
    String expectedValue = "zz";
    assertThat(actualValue).as("Config entry value is wrong").isEqualTo(expectedValue);
  }

  @Test
  public void getExistingConfig() {
    clock.setFixedTime("2026-04-10T10:00:00Z");
    // Arrange: Create active user in database that has some config entry.
    User user = userFactory.genRandUser(EnUserStatus.ACTIVE);
    user.addConfig(userConfigFactory.genConfig(user, "test.variable", "1"));
    userRepository.save(user);

    // Act: get config that exists.
    String actualValue = userConfigService.get(user, "test.variable", "0");

    // Assert: value is correct.
    String expectedValue = "1";
    assertThat(actualValue).as("Config entry value is wrong").isEqualTo(expectedValue);
  }

  @Test
  public void getExistingConfigAsLong() {
    clock.setFixedTime("2026-04-10T10:00:00Z");
    // Arrange: Create active user in database that has some config entry.
    User user = userFactory.genRandUser(EnUserStatus.ACTIVE);
    user.addConfig(userConfigFactory.genConfig(user, "test.variable", "1"));
    userRepository.save(user);

    // Act: get config as Long.
    Long actualValue = userConfigService.getLong(user, "test.variable", 0L);

    // Assert: value is correct.
    Long expectedValue = 1L;
    assertThat(actualValue).as("Config entry value is wrong").isEqualTo(expectedValue);
  }

  @Test
  public void getInvalidConfigAsLong() {
    clock.setFixedTime("2026-04-10T10:00:00Z");
    // Arrange: Create active user in database that has some config entry.
    User user = userFactory.genRandUser(EnUserStatus.ACTIVE);
    user.addConfig(userConfigFactory.genConfig(user, "test.variable", "aaa")); // not a Long!
    userRepository.save(user);

    // Act: get config as Long, but it is string that cannot be parsed as Long.
    Long actualValue = userConfigService.getLong(user, "test.variable", 666L);

    // Assert: value is correct.
    Long expectedValue = 666L;
    assertThat(actualValue).as("Config entry value is wrong").isEqualTo(expectedValue);
  }

  @Test
  public void getNullConfigAsLong() {
    clock.setFixedTime("2026-04-10T10:00:00Z");
    // Arrange: Create active user in database that has some config entry.
    User user = userFactory.genRandUser(EnUserStatus.ACTIVE);
    user.addConfig(userConfigFactory.genConfig(user, "test.variable", "aaa")); // not a Long!
    userRepository.save(user);

    // Act: get config as Long, but it is string that cannot be parsed as Long.
    Long actualValue = userConfigService.getLong(user, "test.variable", null);

    // Assert: value is correct.
    Long expectedValue = null;
    assertThat(actualValue).as("Config entry value is wrong").isEqualTo(expectedValue);
  }

  //

  @Test
  public void setMissingConfig() {
    clock.setFixedTime("2026-04-10T10:00:00Z");
    User expectedUser = userFactory.genUser(EnUserStatus.ACTIVE);
    expectedUser.addConfig(userConfigFactory.genConfig(expectedUser, "test.variable", "1"));

    // Arrange: Create active user in database that has some config entry.
    User user = userFactory.genUser(EnUserStatus.ACTIVE);
    user.addConfig(userConfigFactory.genConfig(user, "test.variable", "1"));
    user = userRepository.save(user);

    // Act: set config that do not exist.
    userConfigService.set(user, "other.variable", "zz");

    // Prepare expected state.
    expectedUser.addConfig(userConfigFactory.genConfig(expectedUser, "other.variable", "zz"));

    // Assert: Database state.
    transactionTemplate.execute(_ -> {
      // Assert: User state.
      User actualUser = userRepository.findByEmail(expectedUser.getEmail()).orElseThrow();
      userAssert.assertIt(actualUser, expectedUser);
      return null;
    });
  }

  @Test
  public void setExistingConfig() {
    clock.setFixedTime("2026-04-10T10:00:00Z");
    // Arrange: Create active user in database that has some config entry.
    User expectedUser = userFactory.genRandUser(EnUserStatus.ACTIVE);
    expectedUser.addConfig(userConfigFactory.genConfig(expectedUser, "test.variable", "1"));
    User user = userRepository.save(expectedUser);

    // Act: set config that exists.
    userConfigService.set(user, "test.variable", "0");

    // Prepare expected state.
    expectedUser.getConfigs().getFirst().setValue("0");

    // Assert: Database state.
    transactionTemplate.execute(_ -> {
      // Assert: User state.
      User actualUser = userRepository.findByEmail(expectedUser.getEmail()).orElseThrow();
      userAssert.assertIt(actualUser, expectedUser);
      return null;
    });
  }
}
