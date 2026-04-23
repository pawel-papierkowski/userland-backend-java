package org.portfolio.userland.system.config.repositories;

import org.portfolio.userland.system.config.entities.Config;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Database interface for project configuration.
 */
@Repository
public interface ConfigRepository extends JpaRepository<Config, Long> {
  Optional<Config> findByName(String name);
}
