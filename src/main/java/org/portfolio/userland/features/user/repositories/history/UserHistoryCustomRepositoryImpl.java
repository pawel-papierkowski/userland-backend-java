package org.portfolio.userland.features.user.repositories.history;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.portfolio.userland.common.repositories.EntityTableHandling;
import org.portfolio.userland.features.user.dto.admin.history.UserHistoryTableReq;
import org.portfolio.userland.features.user.entities.UserHistory;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of custom repository for user history.
 */
@Repository
public class UserHistoryCustomRepositoryImpl extends EntityTableHandling<UserHistoryTableReq, UserHistory> implements UserHistoryCustomRepository {
  @Override
  protected List<Predicate> generatePredicates(UserHistoryTableReq req, CriteriaBuilder cb, Root<UserHistory> entity) {
    List<Predicate> predicates = new ArrayList<>();
    predicates.add(cb.equal(entity.get("user").get("id"), req.userId())); // obligatory field

    if (req.who() != null) {
      predicates.add(cb.equal(entity.get("who"), req.who()));
    }
    if (req.what() != null) {
      predicates.add(cb.equal(entity.get("what"), req.what()));
    }
    if (req.createdFromAt() != null) {
      predicates.add(cb.greaterThanOrEqualTo(entity.get("createdAt"), req.createdFromAt()));
    }
    if (req.createdToAt() != null) {
      predicates.add(cb.lessThanOrEqualTo(entity.get("createdAt"), req.createdToAt()));
    }
    return predicates;
  }
}
