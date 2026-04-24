package org.portfolio.userland.features.user.repositories;

import java.util.List;
import java.util.Map;

/**
 * More complex queries for User.
 */
public interface UserJwtCustomRepository {
  /**
   * Revokes (deletes) all JWTs except for users possessing specific permission name/value pairs.
   * @param allowedPermissions Map where key is permission name and value is permission value.
   * @return Number of deleted JWT entries.
   */
  int revokeAllTokensExcept(Map<String, List<String>> allowedPermissions);
}
