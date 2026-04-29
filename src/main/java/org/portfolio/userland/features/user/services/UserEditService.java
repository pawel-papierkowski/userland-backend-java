package org.portfolio.userland.features.user.services;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.portfolio.userland.features.user.dto.common.UserDataResp;
import org.portfolio.userland.features.user.dto.edit.UserEditReq;
import org.portfolio.userland.features.user.entities.EnUserHistoryWhat;
import org.portfolio.userland.features.user.entities.User;
import org.portfolio.userland.features.user.exceptions.UserDoesNotExistException;
import org.portfolio.userland.system.auth.AuthHelper;
import org.portfolio.userland.system.auth.details.CustomUserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Business logic for editing of user account.
 */
@Service
@RequiredArgsConstructor
public class UserEditService extends BaseUserService {
  private final PasswordEncoder passwordEncoder;

  /**
   * Change selected fields of user account. This is limited version for editing your own account.
   * @param userEditReq User edit request.
   * @return Updated user data.
   */
  @Transactional
  public UserDataResp edit(UserEditReq userEditReq) {
    CustomUserDetails userDetails = AuthHelper.resolveUserDetails();
    if (userDetails == null) throw new IllegalStateException("User details should exist!");
    User user = userRepository.findByEmail(userDetails.getEmail())
        .orElseThrow(() -> new UserDoesNotExistException(userDetails.getEmail()));

    if (userEditReq.anyPresent()) {
      String params = "";
      LocalDateTime nowAt = clockService.getNowUTC();
      user.setModifiedAt(nowAt);
      if (StringUtils.isNotEmpty(userEditReq.username())) {
        user.setUsername(userEditReq.username());
        params += "username ";
      }
      if (StringUtils.isNotEmpty(userEditReq.password())) {
        user.setPassword(passwordEncoder.encode(userEditReq.password()));
        params += "password ";
      }
      if (StringUtils.isNotEmpty(userEditReq.lang())) {
        user.setLang(userEditReq.lang());
        params += "lang ";
      }

      params = params.trim().replace(" ", ", ");
      user = userRepository.save(user);
      addHistoryEvent(user, nowAt, EnUserHistoryWhat.EDIT, params);
    }

    return userMapper.entityToDataResp(user);
  }
}
