package org.portfolio.userland.features.user.repositories.user;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.portfolio.userland.common.dto.TableMeta;
import org.portfolio.userland.common.services.table.TableHelper;
import org.portfolio.userland.features.user.dto.admin.view.UserTableViewReq;
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
  public List<User> viewPage(UserTableViewReq userTableViewReq) {
    CriteriaBuilder cb = entityManager.getCriteriaBuilder();
    CriteriaQuery<User> cq = cb.createQuery(User.class);
    Root<User> user = cq.from(User.class);

    List<Predicate> predicates = new ArrayList<>();

    // Apply filters.
    if (userTableViewReq.username() != null && !userTableViewReq.username().isBlank()) {
      predicates.add(cb.like(cb.lower(user.get("username")), "%" + userTableViewReq.username().toLowerCase() + "%"));
    }
    if (userTableViewReq.email() != null && !userTableViewReq.email().isBlank()) {
      predicates.add(cb.like(cb.lower(user.get("email")), "%" + userTableViewReq.email().toLowerCase() + "%"));
    }
    if (userTableViewReq.status() != null) {
      predicates.add(cb.equal(user.get("status"), userTableViewReq.status()));
    }
    if (userTableViewReq.locked() != null) {
      predicates.add(cb.equal(user.get("locked"), userTableViewReq.locked()));
    }
    if (userTableViewReq.createdFromAt() != null) {
      predicates.add(cb.greaterThanOrEqualTo(user.get("createdAt"), userTableViewReq.createdFromAt()));
    }
    if (userTableViewReq.createdToAt() != null) {
      predicates.add(cb.lessThanOrEqualTo(user.get("createdAt"), userTableViewReq.createdToAt()));
    }

    if (!predicates.isEmpty()) {
      cq.where(cb.and(predicates.toArray(new Predicate[0])));
    }

    TableMeta tableMeta = TableHelper.prepareTableMeta(userTableViewReq.tableMeta());
    TableHelper.applySorting(cb, cq, user, tableMeta);
    TypedQuery<User> query = entityManager.createQuery(cq);
    TableHelper.applyPagination(query, tableMeta);
    return query.getResultList();
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
