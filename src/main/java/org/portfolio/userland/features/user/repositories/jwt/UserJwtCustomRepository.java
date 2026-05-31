package org.portfolio.userland.features.user.repositories.jwt;

import org.portfolio.userland.features.user.dto.admin.jwt.UserJwtTableReq;
import org.portfolio.userland.features.user.entities.UserJwt;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * More complex queries for <code>UserJwt</code> entity.
 */
public interface UserJwtCustomRepository {
  /**
   * Return total count of entries for given filtering. Field tableMeta does not matter here.
   * @param req User JWT table view request.
   * @return Count of entries.
   */
  Long countEntries(UserJwtTableReq req);

  /**
   * View page of user JWTs. Note: tableMeta must be filled.
   * @param req User JWT table view request.
   * @return Page of user JWT entities.
   */
  List<UserJwt> viewPage(UserJwtTableReq req);

  //

  /**
   * Revokes (deletes) all JWTs except for users possessing specific permission name/value pairs.
   * @param allowedPermissions Map where key is permission name and value is permission value.
   * @return Number of deleted JWT entries.
   */
  int revokeAllTokensExcept(Map<String, Set<String>> allowedPermissions);
}
