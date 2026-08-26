package org.portfolio.userland.system.config;

import org.junit.jupiter.api.Test;
import org.portfolio.userland.system.config.entities.Config;
import org.portfolio.userland.system.config.exceptions.ConfigUnknownException;
import org.portfolio.userland.system.config.repositories.ConfigRepository;
import org.portfolio.userland.system.config.service.ConfigConst;
import org.portfolio.userland.system.config.service.ConfigService;
import org.portfolio.userland.test.base.BaseIntegrationTest;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests <code>ConfigService</code>, including its caching behavior.
 */
public class ConfigServiceTest extends BaseIntegrationTest {
  @Autowired
  private ConfigService configService;
  @Autowired
  private ConfigRepository configRepository;

  /**
   * Insert a config variable directly into the repository.
   * @param name Name of configuration variable.
   * @param value Value of configuration variable.
   */
  private Config insertConfig(String name, String value) {
    Config config = new Config();
    config.setName(name);
    config.setValue(value);
    config.setDescription("-");
    return configRepository.save(config);
  }

  // //////////////////////////////////////////////////////////////////////////

  @Test
  public void retrieveExistingConfigVariable() {
    // Arrange: insert config variable.
    Config expectedConfig = insertConfig(ConfigConst.TEST_VAR, "test.otherVal");

    // Act: Call service.
    String actualValue = configService.get(ConfigConst.TEST_VAR);

    // Assert: Value is correct and not default.
    assertThat(actualValue).isEqualTo("test.otherVal");

    // Assert: This config variable is not affected: configService.get() is read-only.
    Config actualConfig = configRepository.findByName(ConfigConst.TEST_VAR).orElseThrow();
    assertThat(actualConfig).as("Config variable should stay same").isEqualTo(expectedConfig);
  }

  @Test
  public void retrieveMissingConfigVariable() {
    // Arrange: Nothing to arrange, we check missing config entry.

    // Act: Call service. Actual value is always same as default, because this config variable does not exist.
    String actualValue = configService.get(ConfigConst.TEST_VAR);

    // Assert: Value is correct.
    assertThat(actualValue).isEqualTo(ConfigConst.TEST_VAR_DEF);

    // Assert: This config variable is not created in database.
    Boolean found = configRepository.findByName(ConfigConst.TEST_VAR).isPresent();
    assertThat(found).as("Config variable '"+ConfigConst.TEST_VAR+"' should not be present.").isFalse();
  }

  @Test
  public void retrieveUnknownConfigVariable() {
    // Arrange: Nothing to arrange, we check unknown config entry.

    // Act&Assert: Call service. Trying to get config entry with name not on list will cause exception.
    assertThrows(
        ConfigUnknownException.class,
        () -> configService.get("test.unknown")
    );
  }

  //

  @Test
  public void updateExistingConfigVariable() {
    // Arrange: insert config variable.
    Config expectedConfig = insertConfig(ConfigConst.TEST_VAR, ConfigConst.TEST_VAR_DEF);

    // Act: Set config variable. Config variable value is same as default.
    configService.set(ConfigConst.TEST_VAR, "test.newVal");

    // Assert: Value is correct.
    expectedConfig.setValue("test.newVal");
    Config actualConfig = configRepository.findByName(ConfigConst.TEST_VAR).orElseThrow();
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

  //

  @Test
  public void setIfChangedUpdatesExistingConfigVariable() {
    // Arrange: insert config variable.
    Config expectedConfig = insertConfig(ConfigConst.TEST_VAR, ConfigConst.TEST_VAR_DEF);

    // Act: Set config variable to different value.
    int updated = configService.setIfChanged(ConfigConst.TEST_VAR, "test.newVal");

    // Assert: Change was performed.
    assertThat(updated).as("Change must be reported as performed").isEqualTo(1);
    expectedConfig.setValue("test.newVal");
    Config actualConfig = configRepository.findByName(ConfigConst.TEST_VAR).orElseThrow();
    assertThat(actualConfig).as("Config variable should be updated").isEqualTo(expectedConfig);
  }

  @Test
  public void setIfChangedSkipsWhenValueAlreadyEqual() {
    // Arrange: insert config variable.
    Config expectedConfig = insertConfig(ConfigConst.TEST_VAR, ConfigConst.TEST_VAR_DEF);

    // Act: Set config variable to the same value it already has.
    int updated = configService.setIfChanged(ConfigConst.TEST_VAR, ConfigConst.TEST_VAR_DEF);

    // Assert: No change was performed, but no exception was thrown either.
    assertThat(updated).as("No-op must be reported as not performed").isZero();
    Config actualConfig = configRepository.findByName(ConfigConst.TEST_VAR).orElseThrow();
    assertThat(actualConfig).as("Config variable should stay same").isEqualTo(expectedConfig);
  }

  @Test
  public void errSetIfChangedMissingConfigVariable() {
    // Arrange: Nothing to arrange. We are trying to update non-existent config variable.

    // Act & Assert: Missing variable must throw exception (not be silently reported as no-op).
    assertThrows(
        ConfigUnknownException.class,
        () -> configService.setIfChanged("test.nonExistentVar", "test.newVal")
    );
  }

  //

  @Test
  public void getCachesValue() {
    // Arrange: Insert config variable and read it once so it gets cached.
    insertConfig(ConfigConst.TEST_CACHE, ConfigConst.TEST_CACHE_DEF);
    String configVal = configService.get(ConfigConst.TEST_CACHE);

    // Assert: Ensure config is correct.
    assertThat(configVal).isEqualTo(ConfigConst.TEST_CACHE_DEF);

    // Act: Change the value directly in database, bypassing the service (so no eviction happens).
    Config config = configRepository.findByName(ConfigConst.TEST_CACHE).orElseThrow();
    config.setValue("changed");
    configRepository.save(config);
    entityManager.clear();

    // Assert: Service still returns the cached value, proving it was served from cache.
    assertThat(configService.get(ConfigConst.TEST_CACHE)).as("Cached value must be returned").isEqualTo(ConfigConst.TEST_CACHE_DEF);
  }

  @Test
  public void setEvictsCache() {
    // Arrange: Insert config variable and read it once so it gets cached.
    insertConfig(ConfigConst.TEST_CACHE, ConfigConst.TEST_CACHE_DEF);
    String configVal = configService.get(ConfigConst.TEST_CACHE);

    // Assert: Ensure config is correct.
    assertThat(configVal).isEqualTo(ConfigConst.TEST_CACHE_DEF);

    // Act: Change the value directly in database bypassing the service.
    Config config = configRepository.findByName(ConfigConst.TEST_CACHE).orElseThrow();
    config.setValue("changedDirectly");
    configRepository.save(config);
    entityManager.clear();

    // Act: Write through the service. This must evict the cache.
    configService.set(ConfigConst.TEST_CACHE, "written");

    // Assert: Without eviction the stale 'original' would be returned; fresh read must see DB state.
    assertThat(configService.get(ConfigConst.TEST_CACHE)).as("Cache must be evicted by set()").isEqualTo("written");
  }

  @Test
  public void setIfChangedEvictsCache() {
    // Arrange: Insert config variable and read it once so it gets cached.
    insertConfig(ConfigConst.TEST_CACHE, ConfigConst.TEST_CACHE_DEF);
    String configVal = configService.get(ConfigConst.TEST_CACHE);

    // Assert: Ensure config is correct.
    assertThat(configVal).isEqualTo(ConfigConst.TEST_CACHE_DEF);

    // Act: Change the value directly in database bypassing the service.
    Config config = configRepository.findByName(ConfigConst.TEST_CACHE).orElseThrow();
    config.setValue("changedDirectly");
    configRepository.save(config);
    entityManager.clear();

    // Act: Write through the service only if changed. This must evict the cache.
    int updated = configService.setIfChanged(ConfigConst.TEST_CACHE, "written");

    // Assert: Change happened and without eviction the stale 'original' would be returned.
    assertThat(updated).as("Change must be reported as performed").isEqualTo(1);
    assertThat(configService.get(ConfigConst.TEST_CACHE)).as("Cache must be evicted by setIfChanged()").isEqualTo("written");
  }
}
