package org.portfolio.userland.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import lombok.Getter;
import org.jspecify.annotations.NonNull;
import org.portfolio.userland.config.RateLimitProperties.LimitProperties;
import org.portfolio.userland.config.RateLimitProperties.ProfileProperties;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Resolves rate limit profiles based on request path.
 * <p>Encapsulates the logic of mapping an Ant-style path pattern to a named profile,
 * and provides the corresponding {@link BucketConfiguration} and bucket key for that profile.</p>
 * <p>Each profile gets its own bucket per client IP. The bucket key format is
 * {@code clientIp:profileName}, ensuring that brute-forcing one endpoint (e.g. <code>/api/users/login</code>)
 * does not consume quota for another endpoint (e.g. <code>/api/users/view</code>).</p>
 *
 * @see RateLimitProperties
 * @see org.portfolio.userland.system.auth.RateLimitFilter
 */
@Service
public class RateLimitProfileResolver {
  private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

  private final Map<String, List<String>> pathMappings;
  @Getter
  private final String defaultProfile;
  private final Map<String, Supplier<BucketConfiguration>> profileConfigs;

  /**
   * Builds profile configurations from properties.
   * @param properties Rate limit properties containing profiles and path mappings.
   */
  public RateLimitProfileResolver(RateLimitProperties properties) {
    this.pathMappings = properties.pathMappings();
    this.defaultProfile = properties.defaultProfile();
    this.profileConfigs = new HashMap<>();

    properties.profiles().forEach((name, profile) ->
        profileConfigs.put(name, buildConfigurationSupplier(profile)));
  }

  /**
   * Resolves the profile name for a given path. Iterates over the path mappings map
   * (profile name → list of path patterns) and returns the first profile whose patterns
   * match the given path. If no mapping matches, the default profile is returned.
   * @param path Request URI path.
   * @return Resolved profile name.
   */
  public String resolveProfile(@NonNull String path) {
    if (pathMappings != null) {
      for (Map.Entry<String, List<String>> entry : pathMappings.entrySet()) {
        String profileName = entry.getKey();
        List<String> patterns = entry.getValue();
        if (patterns != null && patterns.stream().anyMatch(p -> PATH_MATCHER.match(p, path)))
          return profileName;
      }
    }
    return defaultProfile;
  }

  /**
   * Builds a bucket key that includes the profile name, ensuring separate buckets per profile.
   * @param path Request URI path.
   * @param clientIp Client IP address.
   * @return Bucket key in format {@code clientIp:profileName}.
   */
  public String resolveBucketKey(@NonNull String path, @NonNull String clientIp) {
    return clientIp + ":" + resolveProfile(path);
  }

  /**
   * Returns the {@link BucketConfiguration} supplier for the profile matching the given path.
   * @param path Request URI path.
   * @return Configuration supplier for the matched profile.
   */
  public Supplier<BucketConfiguration> resolveConfig(@NonNull String path) {
    String profile = resolveProfile(path);
    return profileConfigs.get(profile);
  }

  /**
   * Builds a {@link Supplier} that produces a {@link BucketConfiguration} from a profile's limits.
   * @param profile Profile properties containing limit definitions.
   * @return Configuration supplier.
   */
  private Supplier<BucketConfiguration> buildConfigurationSupplier(ProfileProperties profile) {
    List<LimitProperties> limits = profile.limits();
    return () -> {
      var builder = BucketConfiguration.builder();
      for (LimitProperties limit : limits) {
        builder.addLimit(Bandwidth.builder()
            .capacity(limit.capacity())
            .refillIntervally(limit.refillTokens(), Duration.ofMinutes(limit.refillInterval()))
            .build());
      }
      return builder.build();
    };
  }
}
