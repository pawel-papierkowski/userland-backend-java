package org.portfolio.userland.features.user.repositories.config;

import org.portfolio.userland.features.user.dto.admin.config.UserConfigEditReq;
import org.portfolio.userland.features.user.dto.admin.config.UserConfigTableReq;
import org.portfolio.userland.features.user.entities.UserConfig;
import org.portfolio.userland.features.user.exceptions.UserConfigMissingException;

import java.util.List;

/**
 * More complex queries for <code>UserConfig</code> entity.
 */
public interface UserConfigCustomRepository {
  /**
   * Return total count of entries for given filtering. Field tableMeta does not matter here.
   * @param req User config table view request.
   * @return Count of entries.
   */
  Long countEntries(UserConfigTableReq req);

  /**
   * View page of user config entries. Note: tableMeta must be filled.
   * @param req User config table view request.
   * @return Page of user config entities.
   */
  List<UserConfig> viewPage(UserConfigTableReq req);

  //

  /**
   * Check if user config with same name combination already exists.
   * @param editReq User config entry edit request.
   * @return True if name already exists, otherwise false.
   */
  default boolean isRedundant(UserConfigEditReq editReq) {
    return isRedundant(editReq.id(), editReq.userId(), editReq.name());
  }

  /**
   * Check if user config with same name combination already exists.
   * @param id     User config entry identificator. Can be null if new entry.
   * @param userId Identificator of the user owning this config.
   * @param name   Name of the config setting.
   * @return True if name already exists, otherwise false.
   */
  boolean isRedundant(Long id, Long userId, String name);

  /**
   * Adds a new user config entry or updates an existing one.
   * @param editReq User config entry edit request.
   * @return Created/updated user config entity or null if failed to update entity.
   * @throws UserConfigMissingException When cannot find user config entry with given id.
   */
  default UserConfig upsert(UserConfigEditReq editReq) {
    return upsert(editReq.id(), editReq.userId(), editReq.name(), editReq.value());
  }

  /**
   * Adds a new user config entry or updates an existing one.
   * @param id     User config entry identificator to update or null to create a new entry.
   * @param userId Identificator of the user owning this config.
   * @param name   Name of the config setting.
   * @param value  Value of the config setting.
   * @return Created/updated user config entity or null if failed to update entity.
   * @throws UserConfigMissingException When cannot find user config entry with given id.
   */
  UserConfig upsert(Long id, Long userId, String name, String value);
}
