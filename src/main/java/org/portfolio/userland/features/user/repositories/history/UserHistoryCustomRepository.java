package org.portfolio.userland.features.user.repositories.history;

import org.portfolio.userland.features.user.dto.admin.history.UserHistoryTableReq;
import org.portfolio.userland.features.user.entities.UserHistory;

import java.util.List;

/**
 * More complex queries for <code>UserHistory</code> entity.
 */
public interface UserHistoryCustomRepository {
  /**
   * Return total count of entries for given filtering. Field tableMeta does not matter here.
   * @param req User history table view request.
   * @return Count of entries.
   */
  Long countEntries(UserHistoryTableReq req);

  /**
   * View page of user config entries. Note: tableMeta must be filled.
   * @param req User history table view request.
   * @return Page of user history entities.
   */
  List<UserHistory> viewPage(UserHistoryTableReq req);
}
