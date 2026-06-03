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

/**
 * Business logic for editing of user account (both user and user profile).
 * <p>Notes:</p>
 * <ul>
 *   <li>Email change is not handled here, as it requires more complex flow for security reasons (sending email with
 *   confirmation link to new address).</li>
 *   <li>We do not expect frequent/concurrent updates for UserProfile. Locking should not be needed.</li>
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
  public UserDataResp edit(UserEditReq userEditReq) {
    User user = userHelperService.resolveUser(false);
    UserProfile userProfile = userProfileRepository.findById(user.getId()).orElseThrow(); // profile should always exist

    boolean userPresent = userEditReq.userPresent();
    boolean userProfilePresent = userEditReq.userProfilePresent();
    String params = "";
    if (userPresent || userProfilePresent) {
      if (userPresent) params += editUser(userEditReq, user);
      if (userProfilePresent) params += editUserProfile(userEditReq, userProfile);
      params = params.trim().replace(" ", ", ");

      // possible to skip this if we "changed" fields to same value
      if (!params.isEmpty()) {
        LocalDateTime nowAt = clockService.getNowUTC();
        user.setModifiedAt(nowAt);
        user = userRepository.save(user);
        if (userProfilePresent) userProfileRepository.save(userProfile);
        addHistoryEvent(user, nowAt, EnUserHistoryWho.USER, EnUserHistoryWhat.EDIT, params);
      }
    }

    return resolveResponse(user, userProfile);
  }

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

  /**
   * Edit data of <code>User</code> entity.
   * @param userEditReq User edit request.
   * @param user User entity.
   * @return History event params.
   */
  private String editUser(UserEditReq userEditReq, User user) {
    String params = "";
    if (StringUtils.isNotEmpty(userEditReq.username()) && !userEditReq.username().equals(user.getUsername())) {
      user.setUsername(userEditReq.username());
      params += "username ";
    }
    if (StringUtils.isNotEmpty(userEditReq.lang()) && !userEditReq.lang().equals(user.getLang())) {
      user.setLang(userEditReq.lang());
      params += "lang ";
    }
    return params;
  }

  /**
   * Edit data of <code>UserProfile</code> entity.
   * @param userEditReq User edit request.
   * @param userProfile User profile entity.
   * @return History event params.
   */
  private String editUserProfile(UserEditReq userEditReq, UserProfile userProfile) {
    String params = "";
    if (StringUtils.isNotEmpty(userEditReq.profile().name()) && !userEditReq.profile().name().equals(userProfile.getName())) {
      userProfile.setName(userEditReq.profile().name());
      params += "name ";
    }
    if (StringUtils.isNotEmpty(userEditReq.profile().surname()) && !userEditReq.profile().name().equals(userProfile.getSurname())) {
      userProfile.setSurname(userEditReq.profile().surname());
      params += "surname ";
    }
    return params;
  }
}
