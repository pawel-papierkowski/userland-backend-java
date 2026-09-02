package org.portfolio.userland.features.user.repositories.permission;

import org.portfolio.userland.features.user.entities.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Database interface for permissions.
 */
@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {
  /**
   * Find permission by name.
   * @param name Name of permission.
   * @return Permission or empty optional.
   */
  Optional<Permission> findByName(String name);
}
