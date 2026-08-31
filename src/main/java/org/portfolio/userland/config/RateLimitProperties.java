package org.portfolio.userland.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Configuration properties for rate limiting.
 * <p>Reads values from {@code app.rateLimit.*} in YAML configuration.</p>
 * @param active If false, rate limiting is disabled.
 * @param cache Cache properties.
 * @param limits List of limit properties.
 */
@ConfigurationProperties(prefix = "app.rate-limit")
public record RateLimitProperties(
    Boolean active,
    CacheProperties cache,
    List<LimitProperties> limits)
{
  /**
   * Caffeine cache configuration for bucket storage.
   * @param maximumSize Max unique IP addresses tracked simultaneously.
   * @param expireAfterAccess When to clean up idle entries, in minutes.
   */
  public record CacheProperties(long maximumSize, int expireAfterAccess) {}

  /**
   * Single rate limit definition (one bandwidth in a token bucket).
   * @param capacity Capacity of this particular bandwidth in a token bucket.
   * @param refillTokens How many tokens will be refilled?
   * @param refillInterval How often tokens will be refilled in minutes?
   */
  public record LimitProperties(long capacity, long refillTokens, int refillInterval) {}
}
