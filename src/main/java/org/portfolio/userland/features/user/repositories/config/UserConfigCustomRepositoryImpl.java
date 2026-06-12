package org.portfolio.userland.features.user.repositories.config;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import org.portfolio.userland.common.repositories.EntityTableHandling;
import org.portfolio.userland.common.services.clock.ClockService;
import org.portfolio.userland.common.services.security.SecurityGeneratorService;
import org.portfolio.userland.features.user.dto.admin.config.UserConfigTableReq;
import org.portfolio.userland.features.user.entities.User;
import org.portfolio.userland.features.user.entities.UserConfig;
import org.portfolio.userland.features.user.exceptions.UserConfigMissingException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of custom repository for user config.
 */
@Repository
@RequiredArgsConstructor
public class UserConfigCustomRepositoryImpl extends EntityTableHandling<UserConfigTableReq, UserConfig> implements UserConfigCustomRepository {
  private final EntityManager entityManager;

  /** Date & time. */
  private final ClockService clockService;
  /** Generator of random tokens, UUIDs etc. */
  private final SecurityGeneratorService securityGeneratorService;

  @Override
  protected List<Predicate> generatePredicates(UserConfigTableReq req, CriteriaBuilder cb, Root<UserConfig> entity) {
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
  @Transactional(readOnly = true)
  public boolean isRedundant(Long id, Long userId, String name) {
    // Note we ignore entry that we edit (if any).
    String query = """
      SELECT count(uc)
      FROM UserConfig uc
      WHERE (:id IS NULL OR uc.id <> :id) and uc.user.id = :userId and uc.name = :name
    """;
    Long count = entityManager.createQuery(query, Long.class)
        .setParameter("id", id)
        .setParameter("userId", userId)
        .setParameter("name", name)
        .getSingleResult();
    return count > 0;
  }

  @Override
  @Transactional
  public UserConfig upsert(Long id, Long userId, String name, String value) {
    UserConfig userConfig;

    if (id != null) {
      userConfig = entityManager.find(UserConfig.class, id);
      if (userConfig == null) throw new UserConfigMissingException(id);
    } else {
      userConfig = new UserConfig();
      userConfig.setUuid(securityGeneratorService.uuid());
      userConfig.setCreatedAt(clockService.getNowUTC());
      userConfig.setUser(entityManager.getReference(User.class, userId)); // avoid fully loading user entity
    }

    userConfig.setName(name);
    userConfig.setValue(value);
    return entityManager.merge(userConfig);
  }
}
