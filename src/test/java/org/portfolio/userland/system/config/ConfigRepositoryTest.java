package org.portfolio.userland.system.config;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.portfolio.userland.system.config.entities.Config;
import org.portfolio.userland.system.config.service.ConfigService;
import org.portfolio.userland.test.base.BaseIntegrationTest;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests <code>ConfigRepository</code> directly, bypassing any service layer.
 * Focuses on repository-specific behavior, in particular that bulk updates
 * do not leave stale values behind.
 */
public class ConfigRepositoryTest extends BaseIntegrationTest {
  @Autowired
  private EntityManager entityManager;

  @BeforeEach
  public void tearDown() {
    configRepository.deleteAll();
    cacheManager.getCache(ConfigService.CONFIG_CACHE).clear();
  }

  //

  /**
   * Reads config variable value directly from database, bypassing persistence context,
   * to make sure we see what is truly stored, not cached entity.
   * @param name Name of config variable.
   * @return Value stored in database.
   */
  private String getValueFromDatabase(String name) {
    List<?> values = entityManager
        .createNativeQuery("SELECT value FROM aux.config WHERE name = :name", String.class)
        .setParameter("name", name)
        .getResultList();
    assertThat(values).as("Config variable '" + name + "' should exist in database").hasSize(1);
    return values.getFirst().toString();
  }

  //

  @Test
  public void updateExistingConfigVariable() {
    // Arrange: Insert config variable.
    Config config = configFactory.genConfig("test.var", "test.val");
    Config savedConfig = configRepository.save(config);

    // Act & Assert: Load, bulk update and re-read must all happen inside ONE transaction,
    // so they share the same persistence context. Only then can staleness occur at all:
    // without 'clearAutomatically' the entity loaded below would be served from the
    // first-level cache with its old value after the bulk update bypassed it.
    Config actualConfig = transactionTemplate.execute(_ -> {
      // Load the variable into persistence context, so it holds old value on purpose.
      // This simulates situation where entity was read before the update happened.
      configRepository.findByName("test.var").orElseThrow();

      // Bulk update goes straight to database, bypassing persistence context entirely.
      int updatedRows = configRepository.updateValueByName("test.var", "test.newVal");

      // Exactly one row was affected.
      assertThat(updatedRows).as("Exactly one row should be updated").isEqualTo(1);

      // Re-read within same transaction: must return updated value, not stale one.
      return configRepository.findByName("test.var").orElseThrow();
    });

    assertThat(actualConfig.getValue()).as("Value should be updated").isEqualTo("test.newVal");
    assertThat(actualConfig.getId()).as("Id should stay same").isEqualTo(savedConfig.getId());
    assertThat(actualConfig.getDescription()).as("Description should stay same").isEqualTo("-");

    // Assert: Database itself contains updated value, checked without persistence context.
    String dbValue = getValueFromDatabase("test.var");
    assertThat(dbValue).as("Database should contain updated value").isEqualTo("test.newVal");
  }

  @Test
  public void updateDoesNotAffectOtherVariables() {
    // Arrange: insert two config variables.
    configRepository.save(configFactory.genConfig("test.var.one", "test.val.one"));
    configRepository.save(configFactory.genConfig("test.var.two", "test.val.two"));

    // Act & Assert: Load, bulk update and re-reads happen inside one shared transaction,
    // so persistence context is involved, and we can check other variables are not affected.
    transactionTemplate.execute(_ -> {
      // Load both variables into persistence context.
      configRepository.findByName("test.var.one").orElseThrow();
      configRepository.findByName("test.var.two").orElseThrow();

      // Update only the first variable.
      int updatedRows = configRepository.updateValueByName("test.var.one", "test.newVal");
      assertThat(updatedRows).as("Exactly one row should be updated").isEqualTo(1);

      // Second variable stays untouched even in the same persistence context.
      Config secondConfig = configRepository.findByName("test.var.two").orElseThrow();
      assertThat(secondConfig.getValue()).as("Second variable should not be affected").isEqualTo("test.val.two");
      return null;
    });

    // Assert: First variable is updated.
    String firstDbValue = getValueFromDatabase("test.var.one");
    assertThat(firstDbValue).as("First variable should be updated").isEqualTo("test.newVal");

    // Assert: Second variable stays untouched, checked without persistence context.
    String secondDbValue = getValueFromDatabase("test.var.two");
    assertThat(secondDbValue).as("Second variable should not be affected").isEqualTo("test.val.two");
  }

  //

  @Test
  public void errUpdateMissingConfigVariable() {
    // Arrange: Nothing to arrange. We are trying to update non-existent config variable.

    // Act: Try to update non-existent config variable.
    int updatedRows = transactionTemplate.execute(_ -> configRepository.updateValueByName("test.nonExistentVar", "test.newVal"));

    // Assert: No rows were affected.
    assertThat(updatedRows).as("No rows should be updated").isZero();

    // Assert: Variable was not created by the update.
    Boolean found = configRepository.findByName("test.nonExistentVar").isPresent();
    assertThat(found).as("Config variable 'test.nonExistentVar' should not be present.").isFalse();
  }
}
