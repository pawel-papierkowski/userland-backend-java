package org.portfolio.userland.features.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.portfolio.userland.features.config.entities.UlConfig;
import org.portfolio.userland.features.config.repositories.UlConfigRepository;
import org.portfolio.userland.features.config.service.UlConfigService;
import org.portfolio.userland.test.base.BaseIntegrationTest;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies working of <code>UlConfigService</code>.
 */
public class UlConfigServiceTest extends BaseIntegrationTest {
  @Autowired
  private UlConfigService ulConfigService;
  @Autowired
  private UlConfigRepository ulConfigRepository;

  @BeforeEach
  public void tearDown() {
    ulConfigRepository.deleteAll();
  }

  //

  @Test
  public void retrieveExistingConfigVariable() {
    // Arrange: insert config variable.
    UlConfig ulConfig = new UlConfig();
    ulConfig.setName("test.var");
    ulConfig.setValue("test.val");
    ulConfig.setDescription("-");
    UlConfig expectedUlConfig = ulConfigRepository.save(ulConfig);

    // Act: Call service.
    // Config variable value is same as default.
    String actualValue = ulConfigService.get("test.var", "test.val");
    String expectedValue = "test.val";

    // Assert: Value is correct.
    assertThat(actualValue).isEqualTo(expectedValue);

    // Assert: This config variable is not affected.
    UlConfig actualUlConfig = ulConfigRepository.findByName("test.var").orElseThrow();
    assertThat(actualUlConfig).isEqualTo(expectedUlConfig);
  }

  @Test
  public void retrieveCustomConfigVariable() {
    // Arrange: insert config variable.
    UlConfig ulConfig = new UlConfig();
    ulConfig.setName("test.var");
    ulConfig.setValue("test.val");
    ulConfig.setDescription("-");
    UlConfig expectedUlConfig = ulConfigRepository.save(ulConfig);

    // Act: Call service.
    // Actual value is different from default.
    String actualValue = ulConfigService.get("test.var", "test.default");
    String expectedValue = "test.val";

    // Assert: Value is correct.
    assertThat(actualValue).isEqualTo(expectedValue);

    // Assert: This config variable is not affected.
    UlConfig actualUlConfig = ulConfigRepository.findByName("test.var").orElseThrow();
    assertThat(actualUlConfig).isEqualTo(expectedUlConfig);
  }

  @Test
  public void retrieveMissingConfigVariable() {
    // Arrange: Nothing to arrange.

    // Act: Call service.
    // Actual value is always same as default, because this config variable does not exist.
    String actualValue = ulConfigService.get("test.non-existent", "test.default");
    String expectedValue = "test.default";

    // Assert: Value is correct.
    assertThat(actualValue).isEqualTo(expectedValue);

    // Assert: This config variable is not created in database.
    assertThat(ulConfigRepository.findByName("test.non-existent").isPresent()).as("Config variable 'test.non-existent' should not be present.").isFalse();
  }

  //
}
