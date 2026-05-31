package org.portfolio.userland.features.user.repositories.token;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.portfolio.userland.common.repositories.EntityTableHandling;
import org.portfolio.userland.features.user.dto.admin.token.UserTokenTableReq;
import org.portfolio.userland.features.user.entities.UserToken;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of custom repository for user token.
 */
@Repository
public class UserTokenCustomRepositoryImpl extends EntityTableHandling<UserTokenTableReq, UserToken> implements UserTokenCustomRepository {
  @Override
  protected List<Predicate> generatePredicates(UserTokenTableReq req, CriteriaBuilder cb, Root<UserToken> entity) {
    List<Predicate> predicates = new ArrayList<>();
    predicates.add(cb.equal(entity.get("user").get("id"), req.userId())); // obligatory field

    if (req.createdFromAt() != null) {
      predicates.add(cb.greaterThanOrEqualTo(entity.get("createdAt"), req.createdFromAt()));
    }
    if (req.createdToAt() != null) {
      predicates.add(cb.lessThanOrEqualTo(entity.get("createdAt"), req.createdToAt()));
    }
    return predicates;
  }
}
