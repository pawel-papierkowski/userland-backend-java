package org.portfolio.userland.system.config.service;

import lombok.RequiredArgsConstructor;
import org.portfolio.userland.system.config.entities.Config;
import org.portfolio.userland.system.config.exceptions.ConfigUnknownException;
import org.portfolio.userland.system.config.repositories.ConfigRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * System configuration service.
 */
@Service
@RequiredArgsConstructor
public class ConfigService {
  private final ConfigRepository configRepository;

  /**
   * Get value of configuration variable.
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
   * Set configuration variable. Note: it must already exist.
   * @param name Name of configuration variable.
   * @param newValue New value of configuration variable.
   */
  public void set(String name, String newValue) {
    Config configEntry = configRepository.findByName(name).orElseThrow(() -> new ConfigUnknownException(name));
    configEntry.setValue(newValue);
    configRepository.saveAndFlush(configEntry);
  }
}
