package org.portfolio.userland.system.history.repositories;

import org.portfolio.userland.system.history.entity.SystemHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Database interface for system history event.
 */
@Repository
public interface SystemHistoryRepository extends JpaRepository<SystemHistory, Long> {
}
