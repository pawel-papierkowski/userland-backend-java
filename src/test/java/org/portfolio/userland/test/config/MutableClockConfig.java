package org.portfolio.userland.test.config;

import org.portfolio.userland.common.services.clock.MutableClock;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Test configuration for mutable clock.
 */
@TestConfiguration
public class MutableClockConfig {
  @Bean
  @Primary
  public MutableClock mutableClock() {
    return new MutableClock();
  }
}
