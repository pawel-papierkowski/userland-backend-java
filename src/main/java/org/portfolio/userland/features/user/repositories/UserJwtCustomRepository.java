package org.portfolio.userland.features.user.repositories;

import java.util.Map;
import java.util.Set;

/**
 * More complex queries for User.
 */
public interface UserJwtCustomRepository {
  /**
   * Revokes (deletes) all JWTs except for users possessing specific permission name/value pairs.
   * @param allowedPermissions Map where key is permission name and value is permission value.
   * @return Number of deleted JWT entries.
   */
  int revokeAllTokensExcept(Map<String, Set<String>> allowedPermissions);
}
