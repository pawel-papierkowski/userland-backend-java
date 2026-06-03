package org.portfolio.userland.features.user.services.admin;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.portfolio.userland.common.dto.TableMetaReq;
import org.portfolio.userland.common.exception.BadParamsException;
import org.portfolio.userland.common.services.table.TableHelper;
import org.portfolio.userland.features.user.dto.admin.user.*;
import org.portfolio.userland.features.user.dto.common.UserProfileData;
import org.portfolio.userland.features.user.entities.EnUserHistoryWhat;
import org.portfolio.userland.features.user.entities.EnUserHistoryWho;
import org.portfolio.userland.features.user.entities.User;
import org.portfolio.userland.features.user.entities.UserProfile;
import org.portfolio.userland.features.user.exceptions.UserCannotEditException;
import org.portfolio.userland.features.user.exceptions.UserEmailAlreadyExistsException;
import org.portfolio.userland.features.user.services.BaseUserService;
import org.portfolio.userland.system.auth.AuthHelper;
import org.portfolio.userland.system.auth.details.CustomUserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
   * @param id Identificator of user.
   * @return Full data about user.
   */
  @Transactional(readOnly = true)
  public UserFullDataResp getUserData(Long id) {
    User user = userHelperService.resolveUser(id, false, false);
    UserProfile userProfile = userProfileRepository.findById(user.getId()).orElseThrow();
    return resolveResponse(user, userProfile);
  }

  // //////////////////////////////////////////////////////////////////////////

  /**
   * Edit user data. This is version for editing someone's else account.
   * @param userFullDataReq User data to change.
   * @return Updated user data.
   */
  public UserFullDataResp editUserData(UserFullDataReq userFullDataReq) {
    verifyRequest(userFullDataReq);
    CustomUserDetails userDetails = AuthHelper.resolveUserDetails();
    if (userDetails == null) return null; // Should not happen.

    User user = userHelperService.resolveUser(userFullDataReq.id(), false, false);
    if (userDetails.getId().equals(user.getId())) // We are not allowed to edit our own account.
      throw new UserCannotEditException(user.getId());

    UserProfile userProfile = userProfileRepository.findById(user.getId()).orElseThrow(); // profile should always exist

    boolean userPresent = userFullDataReq.userPresent();
    boolean userProfilePresent = userFullDataReq.userProfilePresent();
    String params = "";
    if (userPresent || userProfilePresent) {
      if (userPresent) params += updateUser(userFullDataReq, user);
      if (userProfilePresent) params += updateUserProfile(userFullDataReq, userProfile);
      params = params.trim().replace(" ", ", ");

      // possible to skip this if we "changed" fields to same value
      if (!params.isEmpty()) {
        LocalDateTime nowAt = clockService.getNowUTC();
        user.setModifiedAt(nowAt);
        user = userRepository.save(user);
        if (userProfilePresent) userProfileRepository.save(userProfile);
        addHistoryEvent(user, nowAt, EnUserHistoryWho.OPERATOR, EnUserHistoryWhat.EDIT, params);
      }
    }

    return resolveResponse(user, userProfile);
  }

  /**
   * Verify request.
   * @param userFullDataReq User data to change.
   */
  private void verifyRequest(UserFullDataReq userFullDataReq) {
    // Verify if email is valid.
    User user = userRepository.findByEmail(userFullDataReq.email()).orElse(null);
    if (user != null && !user.getId().equals(userFullDataReq.id()))
      throw new UserEmailAlreadyExistsException(userFullDataReq.email());
  }

  /**
   * Actually change user data.
   * @param userFullDataReq User data to change.
   * @param user User entity.
   * @return History event params.
   */
  private String updateUser(UserFullDataReq userFullDataReq, User user) {
    String params = "";
    if (StringUtils.isNotEmpty(userFullDataReq.username()) && !userFullDataReq.username().equals(user.getUsername())) {
      user.setUsername(userFullDataReq.username());
      params += "username ";
    }
    if (StringUtils.isNotEmpty(userFullDataReq.email()) && !userFullDataReq.email().equals(user.getEmail())) {
      user.setEmail(userFullDataReq.email());
      params += "email ";
    }
    if (userFullDataReq.locked() != null && !userFullDataReq.locked().equals(user.getLocked())) {
      user.setLocked(userFullDataReq.locked());
      params += userFullDataReq.locked() ? "locked " : "unlocked ";
    }
    if (StringUtils.isNotEmpty(userFullDataReq.lang()) && !userFullDataReq.lang().equals(user.getLang())) {
      user.setLang(userFullDataReq.lang());
      params += "lang ";
    }
    return params;
  }

  /**
   * Actually change user profile data.
   * @param userFullDataReq User data to change.
   * @param userProfile User profile entity.
   * @return History event params.
   */
  private String updateUserProfile(UserFullDataReq userFullDataReq, UserProfile userProfile) {
    String params = "";
    if (StringUtils.isNotEmpty(userFullDataReq.profile().name()) && !userFullDataReq.profile().name().equals(userProfile.getName())) {
      userProfile.setName(userFullDataReq.profile().name());
      params += "name ";
    }
    if (StringUtils.isNotEmpty(userFullDataReq.profile().surname()) && !userFullDataReq.profile().surname().equals(userProfile.getSurname())) {
      userProfile.setSurname(userFullDataReq.profile().surname());
      params += "surname ";
    }
    return params;
  }

  // //////////////////////////////////////////////////////////////////////////

  /**
   * Generate response.
   * @param user Updated user entity.
   * @param userProfile Updated user profile entity.
   * @return Updated user data.
   */
  private UserFullDataResp resolveResponse(User user, UserProfile userProfile) {
    UserFullDataResp userFullDataResp = userMapper.userToFullDataResp(user);
    UserProfileData userProfileData = userMapper.profileToData(userProfile);
    userFullDataResp = userFullDataResp.toBuilder().profile(userProfileData).build();
    return userFullDataResp;
  }
}
