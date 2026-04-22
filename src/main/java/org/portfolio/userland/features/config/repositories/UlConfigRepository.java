package org.portfolio.userland.features.config.repositories;

import org.portfolio.userland.features.config.entities.UlConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Database interface for UserLand system configuration.
 */
@Repository
public interface UlConfigRepository extends JpaRepository<UlConfig, Long> {
  Optional<UlConfig> findByName(String name);
}
