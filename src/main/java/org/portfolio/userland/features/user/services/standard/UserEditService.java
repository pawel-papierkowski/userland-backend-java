package org.portfolio.userland.features.user.services.standard;

import lombok.RequiredArgsConstructor;
import org.portfolio.userland.features.user.dto.common.UserDataResp;
import org.portfolio.userland.features.user.dto.standard.edit.UserEditReq;
import org.portfolio.userland.features.user.entities.User;
import org.portfolio.userland.features.user.entities.UserProfile;
import org.portfolio.userland.features.user.services.BaseUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
