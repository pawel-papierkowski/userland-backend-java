package org.portfolio.userland.features.user.repositories;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Set;

@Repository
public class UserJwtCustomRepositoryImpl implements UserJwtCustomRepository {

  @PersistenceContext
  private EntityManager entityManager;

  @Override
  @Transactional
  public int revokeAllTokensExcept(Map<String, Set<String>> allowedPermissions) {
    // If the map is empty, apply total lockdown (delete all tokens).
    if (allowedPermissions == null || allowedPermissions.isEmpty()) {
      return entityManager.createQuery("DELETE FROM UserJwt").executeUpdate();
    }

    // Build the dynamic JPQL query using a NOT IN subquery.
    // In English, this query says:
    // "delete all records from UserJwt except ones that belong to users with given permissions".
    StringBuilder jpql = new StringBuilder();
    jpql.append("DELETE FROM UserJwt j WHERE j.user.id NOT IN (");
    jpql.append("  SELECT up.user.id FROM UserPermission up ");
    jpql.append("  WHERE ");

    int index = 0;
    for (Map.Entry<String, Set<String>> entry : allowedPermissions.entrySet()) {
      int size = entry.getValue().size();
      for (int i = 0; i < size; i++) {
        if (index > 0) jpql.append(" OR ");
        jpql.append("(up.permission.name = :name")
            .append(index)
            .append(" AND up.value = :val")
            .append(index)
            .append(")");
        index++;
      }
    }
    jpql.append(")");

    Query query = entityManager.createQuery(jpql.toString());

    // Bind the parameters securely to prevent injection.
    index = 0;
    for (Map.Entry<String, Set<String>> entry : allowedPermissions.entrySet()) {
      Set<String> values = entry.getValue();
      for (String permValue : values) {
        query.setParameter("name" + index, entry.getKey());
        query.setParameter("val" + index, permValue);
        index++;
      }
    }

    // Execute the bulk delete and return the affected row count
    return query.executeUpdate();
  }
}
