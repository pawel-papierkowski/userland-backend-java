package org.portfolio.userland.features.config.service;

import lombok.RequiredArgsConstructor;
import org.portfolio.userland.features.config.entities.UlConfig;
import org.portfolio.userland.features.config.repositories.UlConfigRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * UserLand configuration service.
 */
@Service
@RequiredArgsConstructor
public class UlConfigService {
  private final UlConfigRepository ulConfigRepository;

  /**
   * Get value of configuration variable.
   * @param name Name of configuration variable.
   * @param defaultValue Returns this if configuration variable is missing.
   * @return Value of configuration variable.
   */
  public String get(String name, String defaultValue) {
    Optional<UlConfig> configEntryOpt = ulConfigRepository.findByName(name);
    if (configEntryOpt.isPresent()) return configEntryOpt.get().getValue();
    return defaultValue;
  }

  // TODO implement set (changing config var value)
}
