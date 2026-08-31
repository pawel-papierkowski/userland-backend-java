package org.portfolio.userland.features.user.services.admin;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.portfolio.userland.common.dto.TableMetaReq;
import org.portfolio.userland.common.exception.ShouldNeverHappenException;
import org.portfolio.userland.common.services.table.TableHelper;
import org.portfolio.userland.features.user.dto.admin.user.*;
import org.portfolio.userland.features.user.dto.common.UserProfileData;
import org.portfolio.userland.features.user.entities.EnUserHistoryWhat;
import org.portfolio.userland.features.user.entities.EnUserHistoryWho;
import org.portfolio.userland.features.user.entities.User;
import org.portfolio.userland.features.user.entities.UserProfile;
import org.portfolio.userland.features.user.exceptions.UserCannotEditException;
import org.portfolio.userland.features.user.exceptions.UserEmailAlreadyExistsException;
import org.portfolio.userland.system.auth.AuthHelper;
import org.portfolio.userland.system.auth.details.CustomUserDetails;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Business logic for viewing data of user table.
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
   * @param user User. Can be null.
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

  //

  /**
   * Change user/user profile data.
   * @param userFullDataReq User data to change.
   * @param user User entity.
   * @return History event params.
   */
  private User updateUserData(UserFullDataReq userFullDataReq, User user, UserProfile userProfile) {
    boolean userPresent = userFullDataReq.userPresent();
    boolean userProfilePresent = userFullDataReq.userProfilePresent();

    Set<String> changedFields = new TreeSet<> (); // we need deterministic ordering
    if (userPresent || userProfilePresent) {
      if (userPresent) updateUser(userFullDataReq, user, changedFields);
      if (userProfilePresent) updateUserProfile(userFullDataReq, userProfile, changedFields);

      // possible to skip this if we "changed" fields to same value
      if (!changedFields.isEmpty()) {
        // Note: modifiedAt is normally maintained automatically by JPA auditing, but here we set it explicitly because
        // (a) auditing stamps the field only at flush time, which is too late - response below is built from the
        //     in-memory entity and must carry the new value already;
        // (b) auditing cannot see changes to UserProfile (separate entity), yet business rules require bumping
        //     modifiedAt when profile changes. Setting the field explicitly also guarantees an UPDATE is issued.
        LocalDateTime nowAt = clockService.getNowUTC();
        user.setModifiedAt(nowAt);
        try {
          user = userRepository.save(user);
          if (userProfilePresent) userProfileRepository.save(userProfile);
          // Version is incremented by Hibernate only at flush time (like auditing timestamps), which would be too late -
          // response below is built from the in-memory entities and must already carry the new versions.
          userRepository.flush();
        } catch (DataIntegrityViolationException ex) {
          // The existsByEmail check in verifyRequest() was only a fast path - the real guard is the unique constraint
          // on users.email. We lost a race against a concurrent change that took this email first. Abort transaction
          // immediately (persistence context is inconsistent).
          throw new UserEmailAlreadyExistsException(userFullDataReq.email());
        }
        addHistoryEvent(user, nowAt, EnUserHistoryWho.OPERATOR, EnUserHistoryWhat.EDIT, String.join(", ", changedFields));
      }
    }

    // If email changed, we need to clear all JWTs.
    if (changedFields.contains("email")) userJwtRepository.deleteAllByUser(user.getId());
    return user;
  }

  /**
   * Actually change user data.
   * @param userFullDataReq User data to change.
   * @param user User entity.
   * @param changedFields Set of affected fields.
   */
  private void updateUser(UserFullDataReq userFullDataReq, User user, Set<String> changedFields) {
    if (StringUtils.isNotEmpty(userFullDataReq.username()) && !userFullDataReq.username().equals(user.getUsername())) {
      user.setUsername(userFullDataReq.username());
      changedFields.add("username");
    }
    if (StringUtils.isNotEmpty(userFullDataReq.email()) && !userFullDataReq.email().equals(user.getEmail())) {
      user.setEmail(userFullDataReq.email());
      changedFields.add("email");
    }
    if (userFullDataReq.locked() != null && !userFullDataReq.locked().equals(user.getLocked())) {
      user.setLocked(userFullDataReq.locked());
      changedFields.add(userFullDataReq.locked() ? "locked" : "unlocked");
    }
    if (StringUtils.isNotEmpty(userFullDataReq.lang()) && !userFullDataReq.lang().equals(user.getLang())) {
      user.setLang(userFullDataReq.lang());
      changedFields.add("lang");
    }
  }

  /**
   * Actually change user profile data.
   * @param userFullDataReq User data to change.
   * @param userProfile User profile entity.
   * @param changedFields Set of affected fields.
   */
  private void updateUserProfile(UserFullDataReq userFullDataReq, UserProfile userProfile, Set<String> changedFields) {
    if (StringUtils.isNotEmpty(userFullDataReq.profile().name()) && !userFullDataReq.profile().name().equals(userProfile.getName())) {
      userProfile.setName(userFullDataReq.profile().name());
      changedFields.add("name");
    }
    if (StringUtils.isNotEmpty(userFullDataReq.profile().surname()) && !userFullDataReq.profile().surname().equals(userProfile.getSurname())) {
      userProfile.setSurname(userFullDataReq.profile().surname());
      changedFields.add("surname");
    }
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
