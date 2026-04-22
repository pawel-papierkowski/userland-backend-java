package org.portfolio.userland.features.user.repositories;

import org.portfolio.userland.features.user.entities.UserPermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Database interface for user permission entry.
 */
@Repository
public interface UserPermissionRepository extends JpaRepository<UserPermission, Long> {
}
