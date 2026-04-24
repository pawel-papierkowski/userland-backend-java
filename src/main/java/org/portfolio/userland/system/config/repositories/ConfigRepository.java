package org.portfolio.userland.system.config.repositories;

import org.portfolio.userland.system.config.entities.Config;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Database interface for project configuration.
 */
@Repository
public interface ConfigRepository extends JpaRepository<Config, Long> {
  Optional<Config> findByName(String name);

  /**
   * Updates the value of a configuration variable by its name.
   * Remember, if you loaded same config variable before this call, change won't be reflected until it is explicitly
   * refreshed.
   * @param name The name of the configuration variable to update.
   * @param newValue The new value to set.
   * @return The number of rows affected (should be 1 if found, 0 if not).
   */
  @Modifying
  @Query("UPDATE Config c SET c.value = :newValue WHERE c.name = :name")
  int updateValueByName(String name, String newValue);
}
