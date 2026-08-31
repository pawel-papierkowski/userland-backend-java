package org.portfolio.userland.features.user.services.standard;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.portfolio.userland.features.user.dto.common.UserDataResp;
import org.portfolio.userland.features.user.dto.standard.edit.UserEditReq;
import org.portfolio.userland.features.user.entities.EnUserHistoryWhat;
import org.portfolio.userland.features.user.entities.EnUserHistoryWho;
import org.portfolio.userland.features.user.entities.User;
import org.portfolio.userland.features.user.entities.UserProfile;
import org.portfolio.userland.features.user.services.BaseUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.TreeSet;

/**
 * Business logic for editing of your own user account (both user and user profile). As such, only some fields are
 * allowed to be changed.
 * <p>Notes:</p>
 * <ul>
 *   <li>Email change is not handled here, as it requires more complex flow for security reasons (sending email with
 *   confirmation link to new address).</li>
 *   <li>Optimistic locking: client must send version of user data as read last. If it does not match, request fails
 *   with 409 Conflict. Note version of <code>User</code> acts as umbrella version for whole account - any profile
 *   edit is accompanied by update of user row (see below), so its version changes too. Concurrent modification of
 *   profile row itself is still caught by <code>@Version</code> on <code>UserProfile</code> at flush time.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class UserEditService extends BaseUserService {
  /**
   * Change certain fields of user and user profile. This is version for editing your own account.
   * @param userEditReq User edit request.
   * @return Updated user data.
   */
  @Transactional
  public UserDataResp editUserData(UserEditReq userEditReq) {
    User user = userHelperService.resolveUser(false);

    verifyVersion(userEditReq.version(), user);

    UserProfile userProfile = userProfileRepository.findById(user.getId()).orElseThrow(); // profile should always exist
    user = updateUserData(userEditReq, user, userProfile);
    return resolveResponse(user, userProfile);
  }

  //

  /**
   * Change user/user profile data.
   * @param userEditReq User data to change.
   * @param user User entity.
   * @return History event params.
   */
  private User updateUserData(UserEditReq userEditReq, User user, UserProfile userProfile) {
    boolean userPresent = userEditReq.userPresent();
    boolean userProfilePresent = userEditReq.userProfilePresent();

    Set<String> changedFields = new TreeSet<>(); // we need deterministic ordering
    if (userPresent || userProfilePresent) {
      if (userPresent) updateUser(userEditReq, user, changedFields);
      if (userProfilePresent) updateUserProfile(userEditReq, userProfile, changedFields);

      // possible to skip this if we "changed" fields to same value
      if (!changedFields.isEmpty()) {
        // Note: modifiedAt is normally maintained automatically by JPA auditing, but here we set it explicitly because
        // (a) auditing stamps the field only at flush time, which is too late - response below is built from the
        //     in-memory entity and must carry the new value already;
        // (b) auditing cannot see changes to UserProfile (separate entity), yet business rules require bumping
        //     modifiedAt when profile changes. Setting the field explicitly also guarantees an UPDATE is issued.
        LocalDateTime nowAt = clockService.getNowUTC();
        user.setModifiedAt(nowAt);
        user = userRepository.save(user);
        if (userProfilePresent) userProfileRepository.save(userProfile);
        // Version is incremented by Hibernate only at flush time (like auditing timestamps), which would be too late -
        // response below is built from the in-memory entities and must already carry the new versions.
        userRepository.flush();
        addHistoryEvent(user, nowAt, EnUserHistoryWho.USER, EnUserHistoryWhat.EDIT, String.join(", ", changedFields));
      }
    }
    return user;
  }

  /**
   * Actually change user data.
   * @param userEditReq User edit request.
   * @param user User entity.
   * @param changedFields Set of affected fields.
   */
  private void updateUser(UserEditReq userEditReq, User user, Set<String> changedFields) {
    if (StringUtils.isNotEmpty(userEditReq.username()) && !userEditReq.username().equals(user.getUsername())) {
      user.setUsername(userEditReq.username());
      changedFields.add("username");
    }
    if (StringUtils.isNotEmpty(userEditReq.lang()) && !userEditReq.lang().equals(user.getLang())) {
      user.setLang(userEditReq.lang());
      changedFields.add("lang");
    }
  }

  /**
   * Actually change user profile data.
   * @param userEditReq User edit request.
   * @param userProfile User profile entity.
   * @param changedFields Set of affected fields.
   */
  private void updateUserProfile(UserEditReq userEditReq, UserProfile userProfile, Set<String> changedFields) {
    if (StringUtils.isNotEmpty(userEditReq.profile().name()) && !userEditReq.profile().name().equals(userProfile.getName())) {
      userProfile.setName(userEditReq.profile().name());
      changedFields.add("name");
    }
    if (StringUtils.isNotEmpty(userEditReq.profile().surname()) && !userEditReq.profile().surname().equals(userProfile.getSurname())) {
      userProfile.setSurname(userEditReq.profile().surname());
      changedFields.add("surname");
    }
  }

  // //////////////////////////////////////////////////////////////////////////

  /**
   * Generate response.
   * @param user Updated user entity.
   * @param userProfile Updated user profile entity. Can be null if no changes happened.
   * @return Updated user data.
   */
  private UserDataResp resolveResponse(User user, UserProfile userProfile) {
    UserDataResp userDataResp = userMapper.userToDataResp(user);
    if (userProfile != null) userDataResp = userDataResp.toBuilder().profile(userMapper.profileToData(userProfile)).build();
    return userDataResp;
  }
}
