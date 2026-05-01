package org.portfolio.userland.features.user.services;

import lombok.RequiredArgsConstructor;
import org.portfolio.userland.features.user.dto.common.UserDataResp;
import org.portfolio.userland.features.user.entities.User;
import org.portfolio.userland.features.user.entities.UserProfile;
import org.portfolio.userland.system.auth.AuthHelper;
import org.portfolio.userland.system.auth.details.CustomUserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business logic for viewing data of user account (both user and user profile).
 */
@Service
@RequiredArgsConstructor
public class UserViewService extends BaseUserService {
  /**
   * View certain fields of user and user profile. This is version for viewing your own account.
   * @return User and user profile data.
   */
  @Transactional
  public UserDataResp view() {
    CustomUserDetails userDetails = AuthHelper.resolveUserDetails();
    if (userDetails == null) throw new IllegalStateException("User details should exist!");
    User user = userHelperService.resolveUser(userDetails.getEmail(), false);
    UserProfile userProfile = userProfileRepository.findById(user.getId()).orElseThrow();

    UserDataResp userDataResp = userMapper.userToDataResp(user);
    userDataResp = userDataResp.toBuilder().profile(userProfileMapper.userToDataResp(userProfile)).build();
    return userDataResp;
  }
}
