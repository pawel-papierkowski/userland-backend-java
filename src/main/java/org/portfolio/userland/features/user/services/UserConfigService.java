package org.portfolio.userland.features.user.services;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.portfolio.userland.features.user.entities.User;
import org.portfolio.userland.features.user.entities.UserConfig;
import org.portfolio.userland.features.user.repositories.UserConfigRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Business logic for user configuration.
 */
@Service
@RequiredArgsConstructor
public class UserConfigService extends BaseUserService {
  private final UserConfigRepository userConfigRepository;

  /**
   * Get value of configuration variable.
   * @param name Name of configuration variable.
   * @param defaultValue Returns this if configuration variable is missing.
   * @return Value of configuration variable.
   */
  @Transactional
  public String get(User user, String name, String defaultValue) {
    Optional<UserConfig> configEntryOpt = userConfigRepository.findByUserIdAndName(user.getId(), name);
    if (configEntryOpt.isPresent()) return configEntryOpt.get().getValue();
    return defaultValue;
  }

  /**
   * Get value of configuration variable as Long.
   * @param name Name of configuration variable.
   * @param defaultValue Returns this if configuration variable is missing.
   * @return Value of configuration variable as Long. If value cannot be presented as Long, returns defaultValue.
   */
  @Transactional
  public Long getLong(User user, String name, Long defaultValue) {
    String defaultValueStr = defaultValue == null ? null : defaultValue.toString();
    String valueStr = get(user, name, defaultValueStr);
    if (StringUtils.isEmpty(valueStr)) return defaultValue;
    Long value = defaultValue;
    try {
      value = Long.parseLong(valueStr);
    } catch (NumberFormatException ex) {
      // Swallow.
    }
    return value;
  }

  //

  /**
   * Set configuration variable. If it does not exist, it will be created.
   * Warning: if you resolved user.configs before, it might now be stale.
   * @param name Name of configuration variable.
   * @param newValue New value of configuration variable.
   */
  @Transactional
  public void set(User user, String name, String newValue) {
    UserConfig configEntry = userConfigRepository.findByUserIdAndName(user.getId(), name).orElse(null);
    if (configEntry == null) {
      configEntry = new UserConfig();
      configEntry.setUser(user);
      configEntry.setName(name);
    }
    configEntry.setValue(newValue);
    userConfigRepository.saveAndFlush(configEntry);
  }
}
