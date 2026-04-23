package org.portfolio.userland.features.user.exceptions;

import org.portfolio.userland.common.exception.GeneralException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when token is already expired and can't be used.
 */
public class UserTokenExpiredException extends GeneralException {
  private final String tokenString;

  public UserTokenExpiredException(String tokenString) {
    super(tokenString);
    this.tokenString = tokenString;
  }

  @Override
  public HttpStatus getStatus() {
    return HttpStatus.CONFLICT;
  }

  @Override
  public String getTitle() {
    return "User token is expired.";
  }

  @Override
  public String getDetail() {
    return "Token '"+tokenString+"' already expired.";
  }

  @Override
  public String getType() {
    return "https://api.userland.org/errors/user/token/expired";
  }
}
