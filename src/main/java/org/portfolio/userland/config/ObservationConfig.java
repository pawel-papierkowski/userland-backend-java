package org.portfolio.userland.config;

import io.micrometer.observation.ObservationRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Disables Micrometer Observations for the whole application.
 * <p>No metrics or tracing backend is configured (no Prometheus registry, no tracing exporter). The per-request
 * observation bookkeeping (Spring MVC's <code>ServerHttpObservationFilter</code> and Spring Security's
 * <code>ObservationFilterChainDecorator</code> wrapping every filter in the chain) would be pure wasted
 * CPU/allocations.</p>
 * <p>Providing our own {@link ObservationRegistry} bean also makes Boot's observation auto-configuration back off
 * (<code>ObservationAutoConfiguration</code> is <code>@ConditionalOnMissingBean(ObservationRegistry.class)</code>),
 * and Spring Security skips decorating filter chains when the registry is a NOOP one.</p>
 * <p>Note: this removes the <code>http.server.requests</code> metric from <code>/actuator/metrics</code>.
 * JVM/system metrics are unaffected.</p>
 * <p>If a real metrics/tracing backend is introduced later, simply delete this class and adjust <code>spring.yaml</code>
 * config to enable metrics.</p>
 */
@Configuration
public class ObservationConfig {
  /**
   * Provides NOOP observation registry. All observation calls become near-zero cost no-ops.
   * @return Noop observation registry.
   */
  @Bean
  public ObservationRegistry observationRegistry() {
    return ObservationRegistry.NOOP;
  }
}
