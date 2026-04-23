package org.portfolio.userland.system.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.portfolio.userland.system.config.entities.Config;
import org.portfolio.userland.system.config.exceptions.ConfigUnknownException;
import org.portfolio.userland.system.config.repositories.ConfigRepository;
import org.portfolio.userland.system.config.service.ConfigService;
import org.portfolio.userland.test.base.BaseIntegrationTest;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests <code>ConfigService</code>.
 */
public class ConfigServiceTest extends BaseIntegrationTest {
  @Autowired
  private ConfigService configService;
  @Autowired
  private ConfigRepository configRepository;

  @AfterEach
  public void tearDown() {
    configRepository.deleteAll();
  }

  //

  @Test
  public void retrieveExistingConfigVariable() {
    // Arrange: insert config variable.
    Config config = new Config();
    config.setName("test.var");
    config.setValue("test.val");
    config.setDescription("-");
    Config expectedConfig = configRepository.save(config);

    // Act: Call service.
    // Config variable value is same as default.
    String actualValue = configService.get("test.var", "test.val");

    // Assert: Value is correct.
    String expectedValue = "test.val";
    assertThat(actualValue).isEqualTo(expectedValue);

    // Assert: This config variable is not affected.
    Config actualConfig = configRepository.findByName("test.var").orElseThrow();
    assertThat(actualConfig).as("Config variable should stay same").isEqualTo(expectedConfig);
  }

  @Test
  public void retrieveCustomConfigVariable() {
    // Arrange: insert config variable.
    Config config = new Config();
    config.setName("test.var");
    config.setValue("test.val");
    config.setDescription("-");
    Config expectedConfig = configRepository.save(config);

    // Act: Call service.
    // Actual value is different from default.
    String actualValue = configService.get("test.var", "test.default");

    // Assert: Value is correct.
    String expectedValue = "test.val";
    assertThat(actualValue).isEqualTo(expectedValue);

    // Assert: This config variable is not affected.
    Config actualConfig = configRepository.findByName("test.var").orElseThrow();
    assertThat(actualConfig).as("Config variable should stay same").isEqualTo(expectedConfig);
  }

  @Test
  public void retrieveMissingConfigVariable() {
    // Arrange: Nothing to arrange.

    // Act: Call service. Actual value is always same as default, because this config variable does not exist.
    String actualValue = configService.get("test.non-existent", "test.default");
    String expectedValue = "test.default";

    // Assert: Value is correct.
    assertThat(actualValue).isEqualTo(expectedValue);

    // Assert: This config variable is not created in database.
    Boolean found = configRepository.findByName("test.non-existent").isPresent();
    assertThat(found).as("Config variable 'test.non-existent' should not be present.").isFalse();
  }

  //

  @Test
  public void updateExistingConfigVariable() {
    // Arrange: insert config variable.
    Config config = new Config();
    config.setName("test.var");
    config.setValue("test.val");
    config.setDescription("-");
    Config expectedConfig = configRepository.save(config);

    // Act: Set config variable. Config variable value is same as default.
    configService.set("test.var", "test.newVal");

    // Assert: Value is correct.
    expectedConfig.setValue("test.newVal");
    Config actualConfig = configRepository.findByName("test.var").orElseThrow();
    assertThat(actualConfig).as("Config variable should be updated").isEqualTo(expectedConfig);
  }

  @Test
  public void errUpdateMissingConfigVariable() {
    // Arrange: Nothing to arrange. We are trying to update non-existent config variable.

    // Act & Arrange: Try to set config variable and make sure it threw exception.
    assertThrows(
        ConfigUnknownException.class,
        () -> configService.set("test.nonExistentVar", "test.newVal")
    );
  }
}
