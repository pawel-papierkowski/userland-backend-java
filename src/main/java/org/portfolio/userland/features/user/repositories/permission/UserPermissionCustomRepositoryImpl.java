package org.portfolio.userland.features.user.repositories.permission;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import org.portfolio.userland.common.repositories.EntityTableHandling;
import org.portfolio.userland.common.services.security.SecurityGeneratorService;
import org.portfolio.userland.features.user.dto.admin.permission.UserPermissionEntryInfo;
import org.portfolio.userland.features.user.dto.admin.permission.UserPermissionTableReq;
import org.portfolio.userland.features.user.entities.Permission;
import org.portfolio.userland.features.user.entities.User;
import org.portfolio.userland.features.user.entities.UserPermission;
import org.portfolio.userland.features.user.exceptions.PermissionMissingException;
import org.portfolio.userland.features.user.exceptions.UserPermissionMissingException;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of custom repository for user permission.
 */
@RequiredArgsConstructor
public class UserPermissionCustomRepositoryImpl extends EntityTableHandling<UserPermissionTableReq, UserPermission> implements UserPermissionCustomRepository {
  private final EntityManager entityManager;

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

  /**
   * Fetch permission association in the same query to prevent n+1 when mapper accesses it.
   */
  @Override
  protected void addFetches(Root<UserPermission> entity) {
    entity.fetch("permission", JoinType.LEFT);
  }

  //

  @Override
  @Transactional(readOnly = true)
  public UserPermissionEntryInfo findEntryInfo(Long id) {
    try {
      // Get only needed fields, so we do not hydrate full UserPermission and Permission entities.
      return entityManager.createQuery("""
          SELECT new org.portfolio.userland.features.user.dto.admin.permission.UserPermissionEntryInfo(up.user.id, up.permission.name, up.value)
          FROM UserPermission up
          WHERE up.id = :id
        """, UserPermissionEntryInfo.class)
          .setParameter("id", id)
          .getSingleResult();
    } catch (NoResultException ex) {
      throw new UserPermissionMissingException(id);
    }
  }

  @Override
  @Transactional(readOnly = true)
  public boolean isRedundant(Long id, Long userId, String name, String value) {
    // Note we ignore entry that we edit (if any).
    // Comparison is case-insensitive (lower()), so 'role/admin' and 'role/ADMIN' are treated as duplicates.
    String query = """
      SELECT count(up)
      FROM UserPermission up
      WHERE (:id IS NULL OR up.id <> :id) and up.user.id = :userId and lower(up.permission.name) = lower(:name) and lower(up.value) = lower(:value)
    """;
    Long count = entityManager.createQuery(query, Long.class)
        .setParameter("id", id)
        .setParameter("userId", userId)
        .setParameter("name", name)
        .setParameter("value", value)
        .getSingleResult();
    return count > 0;
  }

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
      // Note createdAt is maintained automatically by JPA auditing.
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
   * @return Permission identifier.
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
