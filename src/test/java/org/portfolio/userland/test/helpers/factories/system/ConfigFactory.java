package org.portfolio.userland.test.helpers.factories.system;

import lombok.RequiredArgsConstructor;
import org.portfolio.userland.system.config.entities.Config;
import org.portfolio.userland.test.helpers.factories.BaseFactory;
import org.springframework.stereotype.Service;

/**
 * Generates system configuration entry for tests.
 */
@Service
@RequiredArgsConstructor
public class ConfigFactory extends BaseFactory {
  /**
   * Generate system configuration entry.
   * @param name Name of config entry.
   * @param value Value of config entry.
   * @return Config entry.
   */
  public Config genConfig(String name, String value) {
    Config config = new Config();
    config.setName(name);
    config.setValue(value);
    config.setDescription("-");
    return config;
  }
}
