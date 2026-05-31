package org.portfolio.userland.features.user.repositories.user;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.portfolio.userland.common.services.table.TableHelper;
import org.portfolio.userland.features.user.dto.admin.user.UserTableReq;
import org.portfolio.userland.features.user.entities.User;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of custom repository for user.
 */
public class UserCustomRepositoryImpl implements UserCustomRepository {
  @PersistenceContext
  private EntityManager entityManager;

  @Override
  public Long countEntries(UserTableReq userTableReq) {
    CriteriaBuilder cb = entityManager.getCriteriaBuilder();
    CriteriaQuery<Long> cq = cb.createQuery(Long.class);
    Root<User> user = cq.from(User.class);

    cq.select(cb.count(user));

    List<Predicate> predicates = generatePredicates(userTableReq, cb, user);
    if (!predicates.isEmpty()) {
      cq.where(cb.and(predicates.toArray(new Predicate[0])));
    }

    // Execute the query to get the single result (the count).
    return entityManager.createQuery(cq).getSingleResult();
  }

  @Override
  public List<User> viewPage(UserTableReq userTableReq) {
    CriteriaBuilder cb = entityManager.getCriteriaBuilder();
    CriteriaQuery<User> cq = cb.createQuery(User.class);
    Root<User> user = cq.from(User.class);

    List<Predicate> predicates = generatePredicates(userTableReq, cb, user);
    if (!predicates.isEmpty()) {
      cq.where(cb.and(predicates.toArray(new Predicate[0])));
    }

    TableHelper.applySorting(cb, cq, user, userTableReq.tableMeta());
    TypedQuery<User> query = entityManager.createQuery(cq);
    TableHelper.applyPagination(query, userTableReq.tableMeta());
    return query.getResultList();
  }

  /**
   * Filtering logic.
   * @param userTableReq Filtering data.
   * @param cb Criteria builder.
   * @param user User entity as <code>Root</code>.
   * @return List of predicates that you should assemble using AND.
   */
  private List<Predicate> generatePredicates(UserTableReq userTableReq, CriteriaBuilder cb, Root<User> user) {
    List<Predicate> predicates = new ArrayList<>();
    // Apply filters.
    if (userTableReq.username() != null && !userTableReq.username().isBlank()) {
      predicates.add(cb.like(cb.lower(user.get("username")), "%" + userTableReq.username().toLowerCase() + "%"));
    }
    if (userTableReq.email() != null && !userTableReq.email().isBlank()) {
      predicates.add(cb.like(cb.lower(user.get("email")), "%" + userTableReq.email().toLowerCase() + "%"));
    }
    if (userTableReq.status() != null) {
      predicates.add(cb.equal(user.get("status"), userTableReq.status()));
    }
    if (userTableReq.locked() != null) {
      predicates.add(cb.equal(user.get("locked"), userTableReq.locked()));
    }
    if (userTableReq.createdFromAt() != null) {
      predicates.add(cb.greaterThanOrEqualTo(user.get("createdAt"), userTableReq.createdFromAt()));
    }
    if (userTableReq.createdToAt() != null) {
      predicates.add(cb.lessThanOrEqualTo(user.get("createdAt"), userTableReq.createdToAt()));
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
