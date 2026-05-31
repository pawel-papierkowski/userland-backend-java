package org.portfolio.userland.features.user.repositories.token;

import org.portfolio.userland.features.user.dto.admin.token.UserTokenTableReq;
import org.portfolio.userland.features.user.entities.UserToken;

import java.util.List;

/**
 * More complex queries for <code>UserToken</code> entity.
 */
public interface UserTokenCustomRepository {
  /**
   * Return total count of entries for given filtering. Field tableMeta does not matter here.
   * @param req User token table view request.
   * @return Count of entries.
   */
  Long countEntries(UserTokenTableReq req);

  /**
   * View page of user token entries. Note: tableMeta must be filled.
   * @param req User token table view request.
   * @return Page of user token entities.
   */
  List<UserToken> viewPage(UserTokenTableReq req);
}
