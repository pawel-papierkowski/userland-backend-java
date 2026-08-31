package org.portfolio.userland.system.auth;

import org.portfolio.userland.common.exception.GeneralException;
import org.portfolio.userland.system.auth.constants.AuthErrCode;
import org.springframework.http.HttpStatus;

import java.util.Map;

/**
 * Thrown when a client exceeds the rate limit for a given endpoint.
 * <p>Returns HTTP 429 Too Many Requests with a {@code Retry-After} header indicating
 * how many seconds the client should wait before retrying.</p>
 */
public class RateLimitException extends GeneralException {
  private static final String TYPE = "https://api.userland.org/errors/rate-limit";
  private static final String RETRY_AFTER_HEADER = "Retry-After";

  private final long retryAfterSeconds;

  /**
   * Constructor.
   * @param retryAfterSeconds Seconds until the next token becomes available.
   */
  public RateLimitException(long retryAfterSeconds) {
    super("Rate limit exceeded. Retry after " + retryAfterSeconds + " seconds.");
    this.retryAfterSeconds = retryAfterSeconds;
  }

  @Override
  public HttpStatus getStatus() {
    return HttpStatus.TOO_MANY_REQUESTS;
  }

  @Override
  public String getTitle() {
    return "Too Many Requests";
  }

  @Override
  public String getType() {
    return TYPE;
  }

  @Override
  public String getErrCode() {
    return AuthErrCode.RATE_LIMIT;
  }

  @Override
  public Map<String, String> getCustomHeaders() {
    return Map.of(RETRY_AFTER_HEADER, String.valueOf(retryAfterSeconds));
  }
}
