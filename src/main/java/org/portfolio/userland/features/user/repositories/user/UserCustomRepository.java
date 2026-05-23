package org.portfolio.userland.features.user.repositories.user;

import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface UserCustomRepository {
  /**
   * Delete active users that were inactive for too long.
   * @param cutoffDateAt Cutoff date.
   * @return Number of deleted users.
   */
  int deleteActiveUsers(LocalDateTime cutoffDateAt);
}
