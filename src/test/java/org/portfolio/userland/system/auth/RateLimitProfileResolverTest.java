package org.portfolio.userland.system.auth;

import org.junit.jupiter.api.Test;
import org.portfolio.userland.config.RateLimitProfileResolver;
import org.portfolio.userland.config.RateLimitProperties;
import org.portfolio.userland.config.RateLimitProperties.LimitProperties;
import org.portfolio.userland.config.RateLimitProperties.ProfileProperties;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit test for {@link RateLimitProfileResolver}. No Spring context needed.
 */
class RateLimitProfileResolverTest {
  private static final String DEFAULT_PROFILE = "standard";

  private RateLimitProperties buildProperties(Map<String, List<String>> pathMappings) {
    LimitProperties limit = new LimitProperties(10, 10, 1);
    return new RateLimitProperties(
        true, List.of(), DEFAULT_PROFILE,
        Map.of(
            "strict", new ProfileProperties(List.of(new LimitProperties(5, 5, 1))),
            "standard", new ProfileProperties(List.of(limit))
        ),
        pathMappings,
        new RateLimitProperties.CacheProperties(1000, 10)
    );
  }

  //

  @Test
  void resolveProfileReturnsMatchingProfile() {
    // Arrange
    RateLimitProperties properties = buildProperties(
        Map.of(
            "strict", List.of("/api/users/login", "/api/users/register"),
            "standard", List.of("/api/users/view")
        )
    );

    // Act
    RateLimitProfileResolver resolver = new RateLimitProfileResolver(properties);

    // Assert
    assertThat(resolver.resolveProfile("/api/users/login")).isEqualTo("strict");
    assertThat(resolver.resolveProfile("/api/users/register")).isEqualTo("strict");
    assertThat(resolver.resolveProfile("/api/users/view")).isEqualTo("standard");
  }

  @Test
  void resolveProfileReturnsDefaultWhenNoMatch() {
    // Arrange
    RateLimitProperties properties = buildProperties(
        Map.of("strict", List.of("/api/users/login"))
    );

    // Act
    RateLimitProfileResolver resolver = new RateLimitProfileResolver(properties);

    // Assert
    assertThat(resolver.resolveProfile("/api/users/unknown")).isEqualTo(DEFAULT_PROFILE);
  }

  @Test
  void resolveProfileFirstMatchWins() {
    // Arrange: Use LinkedHashMap to simulate Spring Boot's YAML binding behavior (preserves insertion order).
    Map<String, List<String>> mappings = new LinkedHashMap<>();
    mappings.put("strict", List.of("/api/users/**"));
    mappings.put("standard", List.of("/api/users/view"));

    // Act
    RateLimitProperties properties = buildProperties(mappings);
    RateLimitProfileResolver resolver = new RateLimitProfileResolver(properties);

    // Assert: /api/users/view matches both, but "strict" is iterated first.
    assertThat(resolver.resolveProfile("/api/users/view")).isEqualTo("strict");
  }

  @Test
  void resolveProfileHandlesAntStylePatterns() {
    // Arrange
    RateLimitProperties properties = buildProperties(
        Map.of("strict", List.of("/api/users/password/**"))
    );

    // Act
    RateLimitProfileResolver resolver = new RateLimitProfileResolver(properties);

    // Assert
    assertThat(resolver.resolveProfile("/api/users/password/link")).isEqualTo("strict");
    assertThat(resolver.resolveProfile("/api/users/password/confirm")).isEqualTo("strict");
    assertThat(resolver.resolveProfile("/api/users/other")).isEqualTo(DEFAULT_PROFILE);
  }

  @Test
  void resolveProfileReturnsDefaultWhenPathMappingsEmpty() {
    // Arrange
    RateLimitProperties properties = buildProperties(Map.of());

    // Act
    RateLimitProfileResolver resolver = new RateLimitProfileResolver(properties);

    // Assert
    assertThat(resolver.resolveProfile("/api/users/login")).isEqualTo(DEFAULT_PROFILE);
  }

  @Test
  void resolveProfileReturnsDefaultWhenPathMappingsNull() {
    // Arrange
    RateLimitProperties properties = new RateLimitProperties(
        true, List.of(), DEFAULT_PROFILE,
        Map.of("standard", new ProfileProperties(List.of(
            new LimitProperties(10, 10, 1)
        ))),
        null, // null pathMappings
        new RateLimitProperties.CacheProperties(1000, 10)
    );

    // Act
    RateLimitProfileResolver resolver = new RateLimitProfileResolver(properties);

    // Assert
    assertThat(resolver.resolveProfile("/api/users/login")).isEqualTo(DEFAULT_PROFILE);
  }

  //

  @Test
  void resolveBucketKeyIncludesProfileName() {
    // Arrange
    RateLimitProperties properties = buildProperties(
        Map.of("strict", List.of("/api/users/login"))
    );

    // Act
    RateLimitProfileResolver resolver = new RateLimitProfileResolver(properties);

    // Assert
    assertThat(resolver.resolveBucketKey("/api/users/login", "192.168.1.1"))
        .isEqualTo("192.168.1.1:strict");
    assertThat(resolver.resolveBucketKey("/api/users/view", "192.168.1.1"))
        .isEqualTo("192.168.1.1:standard");
  }
}
