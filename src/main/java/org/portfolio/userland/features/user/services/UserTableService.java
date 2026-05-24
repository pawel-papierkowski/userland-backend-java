package org.portfolio.userland.features.user.services;

import lombok.RequiredArgsConstructor;
import org.portfolio.userland.features.user.dto.admin.view.UserPageResp;
import org.portfolio.userland.features.user.dto.admin.view.UserTableViewReq;
import org.springframework.stereotype.Service;

/**
 * Business logic for viewing data of user table.
 */
@Service
@RequiredArgsConstructor
public class UserTableService {
  /**
   * Get page from user table. Request contains filtering and other (pagination, sorting) data needed to return correct
   * results.
   * @param userTableViewReq User table view request.
   * @return User table data response.
   */
  public UserPageResp getPage(UserTableViewReq userTableViewReq) {
    // TODO STUB
    return null;
  }
}
