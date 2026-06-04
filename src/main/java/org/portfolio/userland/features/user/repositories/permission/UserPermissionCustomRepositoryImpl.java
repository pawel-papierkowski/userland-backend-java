package org.portfolio.userland.features.user.repositories.permission;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import org.portfolio.userland.common.repositories.EntityTableHandling;
import org.portfolio.userland.common.services.clock.ClockService;
import org.portfolio.userland.common.services.security.SecurityGeneratorService;
import org.portfolio.userland.features.user.dto.admin.permission.UserPermissionTableReq;
import org.portfolio.userland.features.user.entities.Permission;
import org.portfolio.userland.features.user.entities.User;
import org.portfolio.userland.features.user.entities.UserPermission;
import org.portfolio.userland.features.user.exceptions.PermissionMissingException;
import org.portfolio.userland.features.user.exceptions.UserPermissionMissingException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of custom repository for user permission.
 */
@Repository
@RequiredArgsConstructor
public class UserPermissionCustomRepositoryImpl extends EntityTableHandling<UserPermissionTableReq, UserPermission> implements UserPermissionCustomRepository {
  private final EntityManager entityManager;

  /** Date & time. */
  private final ClockService clockService;
  /** Generator of random tokens, UUIDs etc. */
  private final SecurityGeneratorService securityGeneratorService;

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

  //

  @Override
  @Transactional
  public UserPermission upsert(Long id, Long userId, String name, String value) {
    UserPermission userPermission;

    if (id != null) {
      userPermission = entityManager.find(UserPermission.class, id);
      if (userPermission == null) throw new UserPermissionMissingException(id);
    } else {
      userPermission = new UserPermission();
      userPermission.setUuid(securityGeneratorService.uuid());
      userPermission.setCreatedAt(clockService.getNowUTC());
      userPermission.setUser(entityManager.getReference(User.class, userId)); // avoid fully loading user entity
    }

    Long permissionId = fetchPermissionIdByName(name);
    userPermission.setPermission(entityManager.getReference(Permission.class, permissionId));
    userPermission.setValue(value);
    return entityManager.merge(userPermission);
  }

  /**
   * Get permission id based on given name.
   * @param name Name of permission.
   * @return Permission identificator.
   */
  private Long fetchPermissionIdByName(String name) {
    try {
      // Make sure we get only id without loading entire Permission entity.
      return entityManager.createQuery("SELECT p.id FROM Permission p WHERE p.name = :name", Long.class)
          .setParameter("name", name)
          .getSingleResult();
    } catch (NoResultException ex) {
      throw new PermissionMissingException(name);
    }
  }
}
