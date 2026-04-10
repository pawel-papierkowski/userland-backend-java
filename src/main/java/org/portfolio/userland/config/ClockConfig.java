package org.portfolio.userland.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Configure Clock as bean. This makes Clock usable in unit tests.
 */
@Configuration
public class ClockConfig {
  @Bean
  public Clock clock() {
    return Clock.systemUTC();
  }
}
