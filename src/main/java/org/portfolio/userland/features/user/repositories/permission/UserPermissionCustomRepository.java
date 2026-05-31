package org.portfolio.userland.features.user.repositories.permission;

import org.portfolio.userland.features.user.dto.admin.permission.UserPermissionTableReq;
import org.portfolio.userland.features.user.entities.UserPermission;

import java.util.List;

/**
 * More complex queries for <code>UserPermission</code> entity.
 */
public interface UserPermissionCustomRepository {
  /**
   * Return total count of entries for given filtering. Field tableMeta does not matter here.
   * @param req User permission table view request.
   * @return Count of entries.
   */
  Long countEntries(UserPermissionTableReq req);

  /**
   * View page of user permissions. Note: tableMeta must be filled.
   * @param req User permission table view request.
   * @return Page of user permission entities.
   */
  List<UserPermission> viewPage(UserPermissionTableReq req);
}
