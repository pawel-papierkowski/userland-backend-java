package org.portfolio.userland.helpers.factories;

import lombok.RequiredArgsConstructor;
import org.portfolio.userland.common.services.clock.ClockService;
import org.portfolio.userland.features.user.data.EnTokenType;
import org.portfolio.userland.features.user.data.User;
import org.portfolio.userland.features.user.data.UserToken;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Generates user history event for tests.
 */
@Service
@RequiredArgsConstructor
public class UserTokenFactory {
  private final ClockService clockService;

  /** How long before confirmation token expires in hours. */
  @Value("${app.user.token.activation.expires}")
  private long confirmationTokenExpires;

  /**
   * Generate user token entry and assign it to user.
   * @param user User.
   * @return User token entry.
   */
  public UserToken genTokenEntry(User user, EnTokenType type, String tokenStr) {
    UserToken userToken = new UserToken();
    userToken.setCreatedAt(clockService.getNowUTC());
    userToken.setExpiresAt(clockService.getNowUTC().plusHours(confirmationTokenExpires));
    userToken.setType(type);
    userToken.setToken(tokenStr);
    user.addToken(userToken);
    return userToken;
  }
}
