package org.portfolio.userland.system.auth;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
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
import org.portfolio.userland.config.RateLimitProperties;
import org.portfolio.userland.config.security.SecurityConfig;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Service;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Rate limiting filter that enforces per-IP token bucket limits on incoming requests.
 * <p>Uses Bucket4j with a Caffeine-backed {@link ProxyManager} to store and enforce token bucket state.
 * Each unique client IP gets its own bucket with configurable capacity and refill rate.</p>
 * <p>When the rate limit is exceeded, delegates to {@link RateLimitException} via
 * {@link HandlerExceptionResolver}, which returns HTTP 429 with a {@code Retry-After} header.</p>
 * <p>Filter placement: this filter should run early in the chain (before authentication) to reject
 * abusive traffic before any expensive processing occurs.</p>
 * <p>This bean is conditional on {@link ProxyManager} being available. In slice tests
 * (e.g. {@code @WebMvcTest}) where {@code RateLimitConfig} is not loaded, this filter is not
 * created and {@link SecurityConfig} must handle its absence gracefully.</p>
 *
 * @see org.portfolio.userland.config.RateLimitConfig
 * @see RateLimitException
 */
@Service
@RequiredArgsConstructor
@ConditionalOnBean(ProxyManager.class) // so it works in slice test context
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {
  private final ProxyManager<String> proxyManager;
  private final Supplier<BucketConfiguration> bucketConfigurationSupplier;
  private final HttpHelperService httpHelperService;
  private final RateLimitProperties rateLimitProperties;

  @Qualifier("handlerExceptionResolver")
  private final HandlerExceptionResolver handlerExceptionResolver;

  @Override
  protected void doFilterInternal(@NonNull HttpServletRequest request,
                                  @NonNull HttpServletResponse response,
                                  @NonNull FilterChain filterChain) throws ServletException, IOException {
    if (!rateLimitProperties.active()) {
      filterChain.doFilter(request, response);
      return;
    }

    String clientIp = httpHelperService.resolveClientIp();
    Bucket bucket = proxyManager.builder().build(clientIp, bucketConfigurationSupplier);

    ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
    if (probe.isConsumed()) {
      filterChain.doFilter(request, response);
      return;
    }

    long retryAfterNanos = probe.getNanosToWaitForRefill();
    long retryAfterSeconds = TimeUnit.NANOSECONDS.toSeconds(retryAfterNanos);
    log.debug("Rate limit exceeded for IP '{}'. Retry after {} seconds.", clientIp, retryAfterSeconds);

    handlerExceptionResolver.resolveException(request, response, null, new RateLimitException(retryAfterSeconds));
  }
}
