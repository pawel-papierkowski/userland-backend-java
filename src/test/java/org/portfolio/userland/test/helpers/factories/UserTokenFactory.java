package org.portfolio.userland.test.helpers.factories;

import lombok.RequiredArgsConstructor;
import org.portfolio.userland.common.services.clock.ClockService;
import org.portfolio.userland.common.services.security.SecurityGeneratorService;
import org.portfolio.userland.features.user.entity.EnTokenType;
import org.portfolio.userland.features.user.entity.User;
import org.portfolio.userland.features.user.entity.UserToken;
import org.portfolio.userland.features.user.services.UserHelperService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Generates user token entry for tests.
 */
@Service
@RequiredArgsConstructor
public class UserTokenFactory {
  private final SecurityGeneratorService securityGeneratorService;
  private final UserHelperService userHelperService;
  private final ClockService clockService;

  /**
   * Generate user token entry and assign it to user.
   * @param user User.
   * @param type Token type.
   * @param tokenStr Token string.
   * @return User token entry.
   */
  public UserToken genTokenEntry(User user, EnTokenType type, String tokenStr) {
    LocalDateTime nowAt = clockService.getNowUTC();

    UserToken userToken = new UserToken();
    userToken.setCreatedAt(nowAt);
    userToken.setExpiresAt(userHelperService.resolveExpiration(nowAt, type));
    userToken.setType(type);
    userToken.setToken(tokenStr == null ? securityGeneratorService.token() : tokenStr);
    user.addToken(userToken);
    return userToken;
  }
}
