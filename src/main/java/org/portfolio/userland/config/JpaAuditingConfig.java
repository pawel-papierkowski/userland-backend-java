package org.portfolio.userland.config;

import org.portfolio.userland.common.services.clock.ClockService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import java.util.Optional;

/**
 * <p>Enables Spring Data JPA auditing for automatic maintenance of entity timestamps (like
 * <code>User.modifiedAt</code> via <code>@LastModifiedDate</code>). This removes the need to set such fields manually,
 * so future code modifying entities cannot forget them.</p>
 * <p>Note: {@link org.springframework.data.jpa.domain.support.AuditingEntityListener} is not aware of our
 * {@link ClockService} by itself, hence custom {@link DateTimeProvider} bean below. It delegates to ClockService, which
 * means the mutable clock used in tests is honored automatically.</p>
 */
@Configuration
@EnableJpaAuditing(dateTimeProviderRef = "clockDateTimeProvider")
public class JpaAuditingConfig {
  /**
   * Provides 'now' for JPA auditing. Delegates to {@link ClockService} instead of reading system time directly.
   * @param clockService Date&time service wrapping application clock.
   * @return Date&time provider returning current UTC date&time.
   */
  @Bean(name = "clockDateTimeProvider")
  public DateTimeProvider clockDateTimeProvider(ClockService clockService) {
    return () -> Optional.of(clockService.getNowUTC());
  }
}
