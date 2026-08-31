package org.portfolio.userland.system.auth;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.portfolio.userland.common.services.web.HttpHelperService;
import org.portfolio.userland.config.RateLimitProfileResolver;
import org.portfolio.userland.config.RateLimitProperties;
import org.portfolio.userland.config.security.SecurityConfig;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Rate limiting filter that enforces per-IP token bucket limits on incoming requests.
 * <p>Uses Bucket4j with a Caffeine-backed {@link ProxyManager} to store and enforce token bucket state.
 * Each unique client IP gets its own bucket with configurable capacity and refill rate.</p>
 * <p>Bucket keys include the resolved profile name ({@code clientIp:profileName}), ensuring
 * separate buckets per path group. For example, brute-forcing {@code /api/users/login} (strict profile)
 * does not consume quota for {@code /api/users/view} (standard profile).</p>
 * <p>When the rate limit is exceeded, delegates to {@link RateLimitException} via
 * {@link HandlerExceptionResolver}, which returns HTTP 429 with a {@code Retry-After} header.</p>
 * <p>Filter placement: this filter should run early in the chain (before authentication) to reject
 * abusive traffic before any expensive processing occurs.</p>
 * <p>This bean is conditional on {@link ProxyManager} being available. In slice tests
 * (e.g. {@code @WebMvcTest}) where {@code RateLimitConfig} is not loaded, this filter is not
 * created and {@link SecurityConfig} must handle its absence gracefully.</p>
 *
 * @see org.portfolio.userland.config.RateLimitConfig
 * @see RateLimitProfileResolver
 * @see RateLimitException
 */
@Service
@RequiredArgsConstructor
@ConditionalOnBean(ProxyManager.class) // so it works in slice test context
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {
  private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

  private final ProxyManager<String> proxyManager;
  private final RateLimitProfileResolver profileResolver;
  private final HttpHelperService httpHelperService;
  private final RateLimitProperties rateLimitProperties;

  @Qualifier("handlerExceptionResolver")
  private final HandlerExceptionResolver handlerExceptionResolver;

  @Override
  protected void doFilterInternal(@NonNull HttpServletRequest request,
                                  @NonNull HttpServletResponse response,
                                  @NonNull FilterChain filterChain) throws ServletException, IOException {
    if (!shouldRateLimit(request)) {
      filterChain.doFilter(request, response);
      return;
    }

    String clientIp = httpHelperService.resolveClientIp();
    String path = request.getRequestURI();
    String bucketKey = profileResolver.resolveBucketKey(path, clientIp);
    Bucket bucket = proxyManager.builder().build(bucketKey, profileResolver.resolveConfig(path));

    ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
    if (probe.isConsumed()) {
      filterChain.doFilter(request, response);
      return;
    }

    long retryAfterNanos = probe.getNanosToWaitForRefill();
    long retryAfterSeconds = TimeUnit.NANOSECONDS.toSeconds(retryAfterNanos);
    String profile = profileResolver.resolveProfile(path);
    log.debug("Rate limit exceeded for IP '{}' (profile '{}'). Retry after {} seconds.", clientIp, profile, retryAfterSeconds);

    handlerExceptionResolver.resolveException(request, response, null, new RateLimitException(retryAfterSeconds));
  }

  /**
   * Determines whether the given request should be rate-limited based on configured path patterns.
   * <p>Evaluation order:</p>
   * <ol>
   *   <li>If path matches any {@code exclude} pattern → skip rate limiting.</li>
   *   <li>Otherwise → apply rate limiting.</li>
   * </ol>
   * @param request The HTTP request to check.
   * @return {@code true} if the request should be rate-limited, {@code false} otherwise.
   */
  private boolean shouldRateLimit(HttpServletRequest request) {
    if (!rateLimitProperties.active()) return false;

    String path = request.getRequestURI();

    List<String> exclude = rateLimitProperties.exclude();
    if (exclude != null && exclude.stream().anyMatch(pattern -> PATH_MATCHER.match(pattern, path))) {
      return false;
    }

    return true;
  }
}
