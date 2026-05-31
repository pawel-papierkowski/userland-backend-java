package org.portfolio.userland.features.user.repositories.permission;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.portfolio.userland.common.repositories.EntityTableHandling;
import org.portfolio.userland.features.user.dto.admin.permission.UserPermissionTableReq;
import org.portfolio.userland.features.user.entities.UserPermission;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of custom repository for user permission.
 */
@Repository
public class UserPermissionCustomRepositoryImpl extends EntityTableHandling<UserPermissionTableReq, UserPermission> implements UserPermissionCustomRepository {
  @Override
  protected List<Predicate> generatePredicates(UserPermissionTableReq req, CriteriaBuilder cb, Root<UserPermission> entity) {
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
