package org.portfolio.userland.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.caffeine.Bucket4jCaffeine;
import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Configuration for rate limiting via Bucket4j with Caffeine backend.
 * <p>Creates a {@link ProxyManager} backed by a dedicated Caffeine cache that stores token bucket state.
 * The proxy manager is used by {@link org.portfolio.userland.system.auth.RateLimitFilter} to resolve
 * per-IP rate limit buckets.</p>
 * <p>Bucket configurations are resolved per request by {@link RateLimitProfileResolver}, which maps
 * path patterns to named profiles with their own limits.</p>
 *
 * @see RateLimitProperties
 * @see RateLimitProfileResolver
 */
@Configuration
@EnableConfigurationProperties(RateLimitProperties.class)
public class RateLimitConfig {
  private final RateLimitProperties properties;

  /**
   * Constructor.
   * @param properties Configuration for rate limiting code.
   */
  public RateLimitConfig(RateLimitProperties properties) {
    this.properties = properties;
  }

  /**
   * Creates a Caffeine-backed proxy manager for distributed token bucket operations.
   * <p>Each bucket is stored as a Caffeine cache entry keyed by client IP address and profileName.
   * Entries are evicted after the configured write-based timeout to prevent unbounded memory growth.</p>
   * @return Proxy manager for rate limit buckets.
   */
  @Bean
  public ProxyManager<String> rateLimitProxyManager() {
    RateLimitProperties.CacheProperties cacheProps = properties.cache();
    Duration duration = Duration.ofMinutes(cacheProps.expireAfterWrite());

    return Bucket4jCaffeine.<String>builderFor(Caffeine.newBuilder()
            .maximumSize(cacheProps.maximumSize()))
        .expirationAfterWrite(
            ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(duration))
        .build();
  }
}
