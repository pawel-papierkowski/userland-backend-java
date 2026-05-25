package org.portfolio.userland.features.user.services.admin;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.portfolio.userland.common.exception.BadParamsException;
import org.portfolio.userland.features.user.dto.admin.edit.UserFullDataReq;
import org.portfolio.userland.features.user.dto.admin.edit.UserFullDataResp;
import org.portfolio.userland.features.user.dto.admin.view.UserPageResp;
import org.portfolio.userland.features.user.dto.admin.view.UserTableEntry;
import org.portfolio.userland.features.user.dto.admin.view.UserTableViewReq;
import org.portfolio.userland.features.user.dto.common.UserProfileDataResp;
import org.portfolio.userland.features.user.entities.User;
import org.portfolio.userland.features.user.entities.UserProfile;
import org.portfolio.userland.features.user.exceptions.UserNotFoundException;
import org.portfolio.userland.features.user.services.BaseUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Business logic for viewing data of user table.
 */
@Service
@RequiredArgsConstructor
public class UserTableService extends BaseUserService {
  /**
   * Get page from user table. Request contains filtering and other (pagination, sorting) data needed to return correct
   * results.
   * @param userTableViewReq User table view request.
   * @return User table data response.
   */
  @Transactional(readOnly = true)
  public UserPageResp getPage(UserTableViewReq userTableViewReq) {
    verifyRequest(userTableViewReq);
    List<User> userPage = userRepository.viewPage(userTableViewReq);
    return cnvUsersToUserPages(userPage);
  }

  /**
   * Verify request. Any error will cause exception.
   * @param userTableViewReq User table view request.
   */
  private void verifyRequest(UserTableViewReq userTableViewReq) {
    if (userTableViewReq.createdFromAt() != null && userTableViewReq.createdToAt() != null) {
      if (userTableViewReq.createdFromAt().isAfter(userTableViewReq.createdToAt()))
        throw new BadParamsException("Field createdFromAt is after createdToAt!");
    }
  }

  /**
   * Converts list of user entities to user entries in response.
   * @param userPage List of users.
   * @return User page response.
   */
  private UserPageResp cnvUsersToUserPages(List<User> userPage) {
    List<UserTableEntry> userEntries = new ArrayList<>();
    for (User user : userPage) {
      UserTableEntry userTableEntry = userMapper.userToUserTableEntry(user);
      userEntries.add(userTableEntry);
    }
    return UserPageResp.builder()
        .users(userEntries)
        .build();
  }

  // //////////////////////////////////////////////////////////////////////////

  /**
   * Get almost all data about user and user profile.
   * @param userFullDataReq Full user data request.
   * @return Full data about user.
   */
  @Transactional(readOnly = true)
  public UserFullDataResp getUserData(@Valid UserFullDataReq userFullDataReq) {
    User user = userRepository.findById(userFullDataReq.id())
        .orElseThrow(() -> new UserNotFoundException(userFullDataReq.id()));
    UserProfile userProfile = userProfileRepository.findById(user.getId()).orElseThrow();

    UserFullDataResp userFullDataResp = userMapper.userToFullDataResp(user);
    UserProfileDataResp userProfileData = userProfileMapper.profileToDataResp(userProfile);
    userFullDataResp = userFullDataResp.toBuilder().profile(userProfileData).build();
    return userFullDataResp;
  }
}
