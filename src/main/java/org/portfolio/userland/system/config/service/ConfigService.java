package org.portfolio.userland.system.config.service;

import lombok.RequiredArgsConstructor;
import org.portfolio.userland.system.config.entities.Config;
import org.portfolio.userland.system.config.exceptions.ConfigUnknownException;
import org.portfolio.userland.system.config.repositories.ConfigRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * System configuration service.
 */
@Service
@RequiredArgsConstructor
public class ConfigService {
  private final ConfigRepository configRepository;

  /**
   * Get value of configuration variable. It won't create missing configuration variable.
   * @param name Name of configuration variable.
   * @param defaultValue Returns this if configuration variable is missing.
   * @return Value of configuration variable.
   */
  public String get(String name, String defaultValue) {
    Optional<Config> configEntryOpt = configRepository.findByName(name);
    if (configEntryOpt.isPresent()) return configEntryOpt.get().getValue();
    return defaultValue;
  }

  /**
   * Set configuration variable. Note: it must already exist. The update itself is performed as a single atomic
   * UPDATE statement, so concurrent writers cannot cause a lost update and no locking is needed.
   * @param name Name of configuration variable.
   * @param newValue New value of configuration variable.
   */
  @Transactional
  public void set(String name, String newValue) {
    int updated = configRepository.updateValueByName(name, newValue);
    if (updated == 0) throw new ConfigUnknownException(name);
  }
}
