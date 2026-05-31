package org.portfolio.userland.features.user.repositories.user;

import org.portfolio.userland.features.user.dto.admin.user.UserTableReq;
import org.portfolio.userland.features.user.entities.User;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface UserCustomRepository {
  /**
   * Return total count of entries for given filtering. Field tableMeta does not matter here.
   * @param userTableReq User table view request.
   * @return Count of entries.
   */
  Long countEntries(UserTableReq userTableReq);

  /**
   * View page of users. Note: tableMeta must be filled.
   * @param userTableReq User table view request.
   * @return Page of user entities.
   */
  List<User> viewPage(UserTableReq userTableReq);

  //

  /**
   * Delete active users that were inactive for too long.
   * @param cutoffDateAt Cutoff date.
   * @return Number of deleted users.
   */
  int deleteActiveUsers(LocalDateTime cutoffDateAt);

}
