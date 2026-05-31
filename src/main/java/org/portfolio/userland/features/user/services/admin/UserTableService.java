package org.portfolio.userland.features.user.services.admin;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.portfolio.userland.common.dto.TableMetaReq;
import org.portfolio.userland.common.exception.BadParamsException;
import org.portfolio.userland.common.services.table.TableHelper;
import org.portfolio.userland.features.user.dto.admin.user.*;
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
   * @param tableReq User table page request.
   * @return User table data response.
   */
  @Transactional(readOnly = true)
  public UserTableResp getPage(UserTableReq tableReq) {
    verifyRequest(tableReq);
    tableReq = prepareRequest(tableReq);
    Long entryCount = userRepository.countEntries(tableReq);
    List<User> userPage = userRepository.viewPage(tableReq);
    return cnvEntitiesToEntries(userPage, tableReq.tableMeta(), entryCount);
  }

  /**
   * Prepare request, adding missing fields where needed.
   * @param tableReq User table view request.
   * @return Modified user table page request.
   */
  private UserTableReq prepareRequest(UserTableReq tableReq) {
    TableMetaReq tableMetaReq = TableHelper.prepareTableMeta(tableReq.tableMeta());
    return tableReq.toBuilder()
        .tableMeta(tableMetaReq)
        .build();
  }

  /**
   * Verify request. Any error will cause exception.
   * @param tableReq User table page request.
   */
  private void verifyRequest(UserTableReq tableReq) {
    if (tableReq.createdFromAt() != null && tableReq.createdToAt() != null) {
      if (tableReq.createdFromAt().isAfter(tableReq.createdToAt()))
        throw new BadParamsException("Field createdFromAt is after createdToAt!");
    }
  }

  /**
   * Converts list of user entities to user entries in response.
   * @param entities List of users.
   * @param tableMetaReq Metadata for table page request.
   * @param entryCount Entry count.
   * @return User page response.
   */
  private UserTableResp cnvEntitiesToEntries(List<User> entities, TableMetaReq tableMetaReq, Long entryCount) {
    List<UserTableEntry> entries = new ArrayList<>();
    for (User entity : entities) {
      entries.add(userMapper.entityToTableEntry(entity));
    }
    return UserTableResp.builder()
        .entries(entries)
        .tableMeta(TableHelper.fillTableMetaResp(tableMetaReq, entryCount))
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
    UserProfileDataResp userProfileData = userMapper.profileToDataResp(userProfile);
    userFullDataResp = userFullDataResp.toBuilder().profile(userProfileData).build();
    return userFullDataResp;
  }
}
