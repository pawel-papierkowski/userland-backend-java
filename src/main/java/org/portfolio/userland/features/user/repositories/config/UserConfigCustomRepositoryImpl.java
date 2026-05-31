package org.portfolio.userland.features.user.repositories.config;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.portfolio.userland.common.repositories.EntityTableHandling;
import org.portfolio.userland.features.user.dto.admin.config.UserConfigTableReq;
import org.portfolio.userland.features.user.entities.UserConfig;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of custom repository for user config.
 */
@Repository
public class UserConfigCustomRepositoryImpl extends EntityTableHandling<UserConfigTableReq, UserConfig> implements UserConfigCustomRepository {
  @Override
  protected List<Predicate> generatePredicates(UserConfigTableReq req, CriteriaBuilder cb, Root<UserConfig> entity) {
    List<Predicate> predicates = new ArrayList<>();
    predicates.add(cb.equal(entity.get("id"), req.userId())); // obligatory field

    if (req.createdFromAt() != null) {
      predicates.add(cb.greaterThanOrEqualTo(entity.get("createdAt"), req.createdFromAt()));
    }
    if (req.createdToAt() != null) {
      predicates.add(cb.lessThanOrEqualTo(entity.get("createdAt"), req.createdToAt()));
    }
    return predicates;
  }
}
