package org.portfolio.userland.features.user.services.admin;

import lombok.RequiredArgsConstructor;
import org.portfolio.userland.common.dto.TableMetaReq;
import org.portfolio.userland.common.exception.ShouldNeverHappenException;
import org.portfolio.userland.common.services.table.TableHelper;
import org.portfolio.userland.features.user.dto.admin.user.*;
import org.portfolio.userland.features.user.dto.common.UserProfileData;
import org.portfolio.userland.features.user.entities.User;
import org.portfolio.userland.features.user.entities.UserProfile;
import org.portfolio.userland.features.user.exceptions.UserCannotEditException;
import org.portfolio.userland.features.user.exceptions.UserEmailAlreadyExistsException;
import org.portfolio.userland.system.auth.AuthHelper;
import org.portfolio.userland.system.auth.details.CustomUserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Business logic for viewing and editing data of user table.
 */
@Service
@RequiredArgsConstructor
public class UserTableService extends BaseUserTableService {
  /**
   * Get page from user table. Request contains filtering and other (pagination, sorting) data needed to return correct
   * results.
   * @param tableReq User table page request.
   * @return User table data response.
   */
  @Transactional(readOnly = true)
  public UserTableResp getPage(UserTableReq tableReq) {
    verifyRequest(tableReq);
    Long entryCount = userRepository.countEntries(tableReq);
    List<User> userPage = userRepository.viewPage(tableReq);
    return cnvEntitiesToEntries(userPage, tableReq.tableMeta(), entryCount);
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
   * @param id Identifier of user.
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
  @Transactional
  public UserFullDataResp editUserData(UserFullDataReq userFullDataReq) {
    User user = userHelperService.resolveUser(userFullDataReq.id(), false, false);

    verifyVersion(userFullDataReq.version(), user);
    verifyRequest(userFullDataReq, user);
    verifyYourOwnAcc(user);

    UserProfile userProfile = userProfileRepository.findById(user.getId()).orElseThrow(); // profile should always exist
    user = updateUserData(userFullDataReq, user, userProfile);
    return resolveResponse(user, userProfile);
  }

  /**
   * Verify request. In particular, will prevent change of email to already existing email.
   * @param userFullDataReq User data to change.
   * @param user User.
   */
  private void verifyRequest(UserFullDataReq userFullDataReq, User user) {
    // No email to modify or no change in email.
    if (userFullDataReq.email() == null || userFullDataReq.email().equals(user.getEmail())) return;

    // Verify if new email is valid.
    boolean emailExists = userRepository.existsByEmail(userFullDataReq.email());
    if (emailExists) throw new UserEmailAlreadyExistsException(userFullDataReq.email());
  }

  /**
   * Check if we attempt to edit our own account in admin panel. If so, throw exception.
   * @param user User to check.
   */
  private void verifyYourOwnAcc(User user) {
    CustomUserDetails userDetails = AuthHelper.resolveUserDetails();
    if (userDetails == null) throw new ShouldNeverHappenException("User details should exist!");

    if (userDetails.getId().equals(user.getId())) // We are not allowed to edit our own account.
      throw new UserCannotEditException(user.getId());
  }

  // //////////////////////////////////////////////////////////////////////////

  /**
   * Generate response.
   * @param user User entity.
   * @param userProfile User profile entity.
   * @return Updated user data.
   */
  private UserFullDataResp resolveResponse(User user, UserProfile userProfile) {
    UserFullDataResp userFullDataResp = userMapper.userToFullDataResp(user);
    UserProfileData userProfileData = userMapper.profileToData(userProfile);
    userFullDataResp = userFullDataResp.toBuilder().profile(userProfileData).build();
    return userFullDataResp;
  }
}
