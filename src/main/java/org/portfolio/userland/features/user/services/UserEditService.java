package org.portfolio.userland.features.user.services;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.portfolio.userland.features.user.dto.common.UserDataResp;
import org.portfolio.userland.features.user.dto.edit.UserEditReq;
import org.portfolio.userland.features.user.entities.EnUserHistoryWhat;
import org.portfolio.userland.features.user.entities.User;
import org.portfolio.userland.features.user.entities.UserProfile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Business logic for editing of user account (both user and user profile).
 * <p>Notes:</p>
 * <ul>
 *   <li>Email change is not handled here. If it is implemented, it will be elsewhere, as it requires more complex flow
 *   (sending email with confirmation link to new address).</li>
 *   <li>We do not expect frequent/concurrent updates for UserProfile. Locking should not be needed.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class UserEditService extends BaseUserService {
  private final PasswordEncoder passwordEncoder;

  /**
   * Change certain fields of user and user profile. This is version for editing your own account.
   * @param userEditReq User edit request.
   * @return Updated user data.
   */
  @Transactional
  public UserDataResp edit(UserEditReq userEditReq) {
    User user = userHelperService.resolveUser(false);
    UserProfile userProfile = null; // will edit user profile only when needed

    boolean userPresent = userEditReq.userPresent();
    boolean userProfilePresent = userEditReq.userProfilePresent();
    String params = "";
    if (userPresent || userProfilePresent) {
      LocalDateTime nowAt = clockService.getNowUTC();
      user.setModifiedAt(nowAt);
      if (userPresent) params += editUser(userEditReq, user);
      if (userProfilePresent) {
        userProfile = userProfileRepository.findById(user.getId()).orElseThrow(); // profile should always exist
        params += editUserProfile(userEditReq, userProfile);
      }

      user = userRepository.save(user);
      if (userProfile != null) userProfileRepository.save(userProfile);
      params = params.trim().replace(" ", ", ");
      addHistoryEvent(user, nowAt, EnUserHistoryWhat.EDIT, params);
    }

    UserDataResp userDataResp = userMapper.userToDataResp(user);
    if (userProfile != null) userDataResp = userDataResp.toBuilder().profile(userProfileMapper.profileToDataResp(userProfile)).build();
    return userDataResp;
  }

  /**
   * Edit data of <code>User</code> entity.
   * @param userEditReq User edit request.
   * @param user User.
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
   * @param userProfile User profile.
   * @return History event params.
   */
  private String editUserProfile(UserEditReq userEditReq, UserProfile userProfile) {
    String params = "";
    if (StringUtils.isNotEmpty(userEditReq.name()) && !userEditReq.name().equals(userProfile.getName())) {
      userProfile.setName(userEditReq.name());
      params += "name ";
    }
    if (StringUtils.isNotEmpty(userEditReq.surname()) && !userEditReq.name().equals(userProfile.getSurname())) {
      userProfile.setSurname(userEditReq.surname());
      params += "surname ";
    }
    return params;
  }
}
