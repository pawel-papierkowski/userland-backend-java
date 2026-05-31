package org.portfolio.userland.features.user.repositories.config;

import org.portfolio.userland.features.user.dto.admin.config.UserConfigTableReq;
import org.portfolio.userland.features.user.entities.UserConfig;

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
}
