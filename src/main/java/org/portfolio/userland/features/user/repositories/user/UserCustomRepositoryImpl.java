package org.portfolio.userland.features.user.repositories.user;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.portfolio.userland.common.repositories.EntityTableHandling;
import org.portfolio.userland.features.user.dto.admin.user.UserTableReq;
import org.portfolio.userland.features.user.entities.User;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of custom repository for user.
 */
public class UserCustomRepositoryImpl extends EntityTableHandling<UserTableReq, User> implements UserCustomRepository {
  @Override
  protected List<Predicate> generatePredicates(UserTableReq req, CriteriaBuilder cb, Root<User> entity) {
    List<Predicate> predicates = new ArrayList<>();
    // Apply filters.
    if (req.username() != null && !req.username().isBlank()) {
      predicates.add(cb.like(cb.lower(entity.get("username")), "%" + req.username().toLowerCase() + "%"));
    }
    if (req.email() != null && !req.email().isBlank()) {
      predicates.add(cb.like(cb.lower(entity.get("email")), "%" + req.email().toLowerCase() + "%"));
    }
    if (req.status() != null) {
      predicates.add(cb.equal(entity.get("status"), req.status()));
    }
    if (req.locked() != null) {
      predicates.add(cb.equal(entity.get("locked"), req.locked()));
    }
    if (req.createdFromAt() != null) {
      predicates.add(cb.greaterThanOrEqualTo(entity.get("createdAt"), req.createdFromAt()));
    }
    if (req.createdToAt() != null) {
      predicates.add(cb.lessThanOrEqualTo(entity.get("createdAt"), req.createdToAt()));
    }
    return predicates;
  }

  //

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
