package org.portfolio.userland.features.user.repositories;

import org.portfolio.userland.features.user.entities.UserConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Database interface for user configuration entry.
 */
@Repository
public interface UserConfigRepository extends JpaRepository<UserConfig, Long> {
  /**
   * Find configuration entry by user and name.
   * @param name Email.
   * @return User configuration entry or empty optional.
   */
  Optional<UserConfig> findByUserIdAndName(Long userId, String name);
}
