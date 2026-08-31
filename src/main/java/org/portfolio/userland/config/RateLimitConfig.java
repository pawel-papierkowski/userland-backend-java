package org.portfolio.userland.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.caffeine.Bucket4jCaffeine;
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;

/**
 * Configuration for rate limiting via Bucket4j with Caffeine backend.
 * <p>Creates a {@link ProxyManager} backed by a dedicated Caffeine cache that stores token bucket state.
 * The proxy manager is used by {@link org.portfolio.userland.system.auth.RateLimitFilter} to resolve
 * per-IP rate limit buckets.</p>
 */
@Configuration
@EnableConfigurationProperties(RateLimitProperties.class)
public class RateLimitConfig {
  private final RateLimitProperties properties;

  public RateLimitConfig(RateLimitProperties properties) {
    this.properties = properties;
  }

  /**
   * Creates a Caffeine-backed proxy manager for distributed token bucket operations.
   * <p>Each bucket is stored as a Caffeine cache entry keyed by client IP address.
   * Entries are evicted after the configured idle timeout to prevent unbounded memory growth.</p>
   * @return Proxy manager for rate limit buckets.
   */
  @Bean
  public ProxyManager<String> rateLimitProxyManager() {
    RateLimitProperties.CacheProperties cacheProps = properties.cache();
    Duration maxIdle = Duration.ofMinutes(cacheProps.expireAfterAccess());

    return Bucket4jCaffeine.<String>builderFor(Caffeine.newBuilder()
            .maximumSize(cacheProps.maximumSize()))
        .expirationAfterWrite(
            ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(maxIdle))
        .build();
  }

  /**
   * Supplies the Bucket4j configuration built from YAML properties.
   * <p>Each call creates a new configuration instance; the proxy manager caches the resulting
   * bucket state, so the configuration supplier is only invoked on first access per key.</p>
   * @return Configuration supplier for per-IP buckets.
   */
  @Bean
  public Supplier<BucketConfiguration> rateLimitBucketConfigurationSupplier() {
    List<RateLimitProperties.LimitProperties> limits = properties.limits();

    return () -> BucketConfiguration.builder()
        .addLimit(buildBandwidth(limits.get(0)))
        .addLimit(buildBandwidth(limits.get(1)))
        .build();
  }

  /**
   * Build a single bandwidth from properties.
   * @param props Limit properties.
   * @return Configured bandwidth.
   */
  private Bandwidth buildBandwidth(RateLimitProperties.LimitProperties props) {
    return Bandwidth.builder()
        .capacity(props.capacity())
        .refillIntervally(props.refillTokens(), Duration.ofMinutes(props.refillInterval()))
        .build();
  }
}
