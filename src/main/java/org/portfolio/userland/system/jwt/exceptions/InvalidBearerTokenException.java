package org.portfolio.userland.system.jwt.exceptions;

import org.portfolio.userland.common.exception.GeneralException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

import java.util.Map;

public class InvalidBearerTokenException extends GeneralException {
  private final String jwtStr;

  public InvalidBearerTokenException(String jwtStr) {
    super(jwtStr);
    this.jwtStr = jwtStr;
  }

  @Override
  public HttpStatus getStatus() {
    return HttpStatus.UNAUTHORIZED;
  }

  @Override
  public String getTitle() {
    return "Unauthorized";
  }

  @Override
  public String getDetail() {
    return "Bearer token is invalid or malformed and cannot be used.";
  }

  @Override
  public String getType() {
    return "https://api.userland.org/errors/user/malformedToken";
  }

  @Override
  public Map<String, String> getCustomHeaders() {
    // RFC 6750 says an expired, revoked, malformed, or otherwise invalid bearer token should produce 401 Unauthorized,
    // and the response should use the invalid_token error value in the WWW-Authenticate header.
    // This header is used to distinguish between "no token provided" and "invalid token provided".
    return Map.of(HttpHeaders.WWW_AUTHENTICATE, "Bearer error=\"invalid_token\"");
  }
}
