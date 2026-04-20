package org.portfolio.userland.features.user.repositories;

import org.portfolio.userland.features.user.entity.UserHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Database interface for user history event.
 */
@Repository
public interface UserHistoryRepository extends JpaRepository<UserHistory, Long> {
}
