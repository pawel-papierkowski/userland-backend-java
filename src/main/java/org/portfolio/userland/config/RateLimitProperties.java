package org.portfolio.userland.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Map;

/**
 * Configuration properties for rate limiting.
 * <p>Reads values from {@code app.rate-limit.*} in YAML configuration.</p>
 * @param active If false, rate limiting is disabled.
 * @param exclude Ant-style path patterns to exclude from rate limiting.
 * @param defaultProfile Name of the profile used for paths without an explicit mapping.
 * @param profiles Named rate limit profiles, each with its own limits.
 * @param pathMappings Profile-to-paths mapping. Each key is a profile name,
 *        value is a list of Ant-style path patterns assigned to that profile.
 * @param cache Cache properties.
 */
@ConfigurationProperties(prefix = "app.rate-limit")
public record RateLimitProperties(
    Boolean active,
    List<String> exclude,
    String defaultProfile,
    Map<String, ProfileProperties> profiles,
    Map<String, List<String>> pathMappings,
    CacheProperties cache)
{
  /**
   * Caffeine cache configuration for bucket storage.
   * @param maximumSize Max unique IP addresses tracked simultaneously.
   * @param expireAfterWrite When to clean up eligible entries (write-based strategy), in minutes.
   */
  public record CacheProperties(long maximumSize, int expireAfterWrite) {}

  /**
   * A named rate limit profile with its own set of bandwidth definitions.
   * @param limits List of limit properties for this profile.
   */
  public record ProfileProperties(List<LimitProperties> limits) {}

  /**
   * Single rate limit definition (one bandwidth in a token bucket).
   * @param capacity Capacity of this particular bandwidth in a token bucket.
   * @param refillTokens How many tokens will be refilled?
   * @param refillInterval How often tokens will be refilled in minutes?
   */
  public record LimitProperties(long capacity, long refillTokens, int refillInterval) {}
}
