package org.portfolio.userland.features.user.repositories.permission;

import org.portfolio.userland.features.user.dto.admin.permission.UserPermissionEditReq;
import org.portfolio.userland.features.user.dto.admin.permission.UserPermissionTableReq;
import org.portfolio.userland.features.user.entities.UserPermission;
import org.portfolio.userland.features.user.exceptions.UserPermissionMissingException;

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

  //

  /**
   * Adds a new user permission entry or updates an existing one.
   * @param editReq User permission entry edit request.
   * @return Created/updated user permission entity or null if failed to update entity.
   * @throws UserPermissionMissingException When cannot find user permission entry with given id.
   */
  default UserPermission upsert(UserPermissionEditReq editReq) {
    return upsert(editReq.id(), editReq.userId(), editReq.name(), editReq.value());
  }

  /**
   * Adds a new user permission entry or updates an existing one.
   * @param id     User permission entry identificator to update or null to create a new entry.
   * @param userId Identificator of the user owning this permission.
   * @param name   Name of the permission setting.
   * @param value  Value of the permission setting.
   * @return Created/updated user permission entity or null if failed to update entity.
   * @throws UserPermissionMissingException When cannot find user permission entry with given id.
   */
  UserPermission upsert(Long id, Long userId, String name, String value);
}
