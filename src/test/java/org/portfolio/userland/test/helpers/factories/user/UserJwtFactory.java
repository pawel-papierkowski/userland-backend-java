package org.portfolio.userland.test.helpers.factories.user;

import lombok.RequiredArgsConstructor;
import org.portfolio.userland.features.user.entities.User;
import org.portfolio.userland.features.user.entities.UserJwt;
import org.portfolio.userland.test.helpers.factories.BaseFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Generates user JWT entry for tests.
 */
@Service
@RequiredArgsConstructor
public class UserJwtFactory extends BaseFactory {
  /**
   * Generate user JWT entry and assign it to user.
   * @param user User.
   * @param jwtStr JWT string.
   * @return User JWT entry.
   */
  public UserJwt genJwtEntry(User user, String jwtStr) {
    return genJwtEntry(user, jwtStr, null);
  }

  /**
   * Generate user JWT entry and assign it to user.
   * @param user User.
   * @param jwtStr JWT string.
   * @param customExpiration Custom expiration in minutes. Can be null, will use default expiration.
   * @return User JWT entry.
   */
  public UserJwt genJwtEntry(User user, String jwtStr, Long customExpiration) {
    LocalDateTime nowAt = clockService.getNowUTC();

    UserJwt userJwt = new UserJwt();
    userJwt.setCreatedAt(nowAt);
    userJwt.setExpiresAt(userHelperService.resolveJwtExpiration(nowAt, customExpiration));
    userJwt.setToken(jwtStr);
    user.addJwt(userJwt);
    return userJwt;
  }
}
