package org.portfolio.userland.system.config.repositories;

import org.portfolio.userland.system.config.entities.Config;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Database interface for system configuration.
 */
@Repository
public interface ConfigRepository extends JpaRepository<Config, Long> {
  Optional<Config> findByName(String name);

  /**
   * Just checks if configuration variable of given name exists.
   * @param name Name of configuration variable.
   * @return True if configuration variable exists, otherwise false.
   */
  boolean existsByName(String name);

  /**
   * Updates the value of a configuration variable by its name.
   * @param name The name of the configuration variable to update.
   * @param newValue The new value to set.
   * @return The number of rows affected (should be 1 if found, 0 if not).
   */
  @Modifying(clearAutomatically=true)
  @Query("UPDATE Config c SET c.value = :newValue WHERE c.name = :name")
  int updateValueByName(String name, String newValue);

  /**
   * Updates the value of a configuration variable by its name, but only if its value differs from the new one
   * (atomic compare-and-set). Safe to be called by multiple concurrent transactions - exactly one of them will get
   * return value of 1, all others will get 0. Intended for cases where the decision "did anything change?" must be
   * made atomically together with write itself.
   * <p>Note: zero rows affected can mean both 'value was already equal' and 'variable is missing' - use
   * {@code existsByName} to distinguish these two cases.</p>
   * @param name The name of the configuration variable to update.
   * @param newValue The new value to set.
   * @return The number of rows affected (1 if value was changed, 0 otherwise).
   */
  @Modifying(clearAutomatically=true)
  @Query("UPDATE Config c SET c.value = :newValue WHERE c.name = :name AND c.value <> :newValue")
  int updateValueByNameIfChanged(String name, String newValue);
}
