package org.portfolio.userland.test.helpers.factories.user;

import lombok.RequiredArgsConstructor;
import org.portfolio.userland.features.user.entities.User;
import org.portfolio.userland.features.user.entities.UserConfig;
import org.springframework.stereotype.Service;

/**
 * Generates user configuration entry for tests.
 */
@Service
@RequiredArgsConstructor
public class UserConfigFactory {
  public UserConfig genConfig(User user, String name, String value) {
    UserConfig userConfig = new UserConfig();
    userConfig.setUser(user);
    userConfig.setName(name);
    userConfig.setValue(value);
    return userConfig;
  }
}
