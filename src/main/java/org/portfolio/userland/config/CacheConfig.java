package org.portfolio.userland.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * <p>Application-wide caching configuration.</p>
 * <p>Uses Caffeine as in-memory cache engine behind the Spring Cache abstraction (@Cacheable/@CacheEvict).
 * Entries expire after write. Caffeine uses timer-wheel eviction.
 * No background refresh threads are used because Cloud Run throttles CPU between requests. Expiry time also bounds
 * cross-instance staleness: on Cloud Run each instance has its own in-memory cache, so a change performed on one
 * instance becomes visible everywhere at worst after the TTL.</p>
 */
@Configuration
@EnableCaching
public class CacheConfig {
  /** Time an entry stays valid after being written. Bounds cross-instance staleness of cached data. */
  private static final Duration EXPIRE_AFTER_WRITE = Duration.ofSeconds(20);
  /** Upper bound of entries per cache. Protects the constrained Cloud Run memory budget. */
  private static final long MAXIMUM_SIZE = 100;

  /**
   * Cache manager backing all @Cacheable operations. Caches are created on demand.
   * @return Configured cache manager.
   */
  @Bean
  public CacheManager cacheManager() {
    CaffeineCacheManager cacheManager = new CaffeineCacheManager();
    cacheManager.setCaffeine(Caffeine.newBuilder()
        .expireAfterWrite(EXPIRE_AFTER_WRITE)
        .maximumSize(MAXIMUM_SIZE));
    return cacheManager;
  }
}
