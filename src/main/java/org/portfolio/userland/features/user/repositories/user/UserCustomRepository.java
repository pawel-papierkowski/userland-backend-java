package org.portfolio.userland.features.user.repositories.user;

import org.portfolio.userland.features.user.dto.admin.view.UserTableViewReq;
import org.portfolio.userland.features.user.entities.User;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface UserCustomRepository {
  /**
   * View page of users.
   * @param userTableViewReq User table view request.
   * @return Page of user entities.
   */
  List<User> viewPage(UserTableViewReq userTableViewReq);

  //

  /**
   * Delete active users that were inactive for too long.
   * @param cutoffDateAt Cutoff date.
   * @return Number of deleted users.
   */
  int deleteActiveUsers(LocalDateTime cutoffDateAt);
}
