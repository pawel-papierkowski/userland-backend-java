package org.portfolio.userland.test.helpers.factories.user;

import lombok.RequiredArgsConstructor;
import org.portfolio.userland.features.user.entities.EnUserTokenType;
import org.portfolio.userland.features.user.entities.User;
import org.portfolio.userland.features.user.entities.UserToken;
import org.portfolio.userland.features.user.services.UserHelperService;
import org.portfolio.userland.test.helpers.factories.BaseFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Generates user token entry for tests.
 */
@Service
@RequiredArgsConstructor
public class UserTokenFactory extends BaseFactory {
  private final UserHelperService userHelperService;

  /**
   * Generate user token entry and assign it to user.
   * @param user User.
   * @param type Token type.
   * @param tokenStr Token string.
   * @return User token entry.
   */
  public UserToken genTokenEntry(User user, EnUserTokenType type, String tokenStr) {
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
