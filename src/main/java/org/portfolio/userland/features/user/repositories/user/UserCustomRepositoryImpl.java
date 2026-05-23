package org.portfolio.userland.features.user.repositories.user;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.time.LocalDateTime;

/**
 * Implementation of custom repository for user.
 */
public class UserCustomRepositoryImpl implements UserCustomRepository {
  @PersistenceContext
  private EntityManager entityManager;

  @Override
  public int deleteActiveUsers(LocalDateTime cutoffDateAt) {
    // Will find users that do NOT have any entries in history that are after cutoff date.
    // This effectively identifies users whose last activity was before the cutoff.
    String query = """
            DELETE FROM User u
            WHERE u.status = 'ACTIVE'
            AND NOT EXISTS (
                SELECT h FROM UserHistory h
                WHERE h.user = u AND h.createdAt >= :cutoffDateAt
            )
            """;
    return entityManager.createQuery(query)
        .setParameter("cutoffDateAt", cutoffDateAt)
        .executeUpdate();
  }
}
