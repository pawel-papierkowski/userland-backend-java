package org.portfolio.userland.features.user.exceptions;

import org.portfolio.userland.common.exception.GeneralException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when token string is missing.
 */
public class UserTokenMissingException extends GeneralException {
  private final String tokenString;

  public UserTokenMissingException(String tokenString) {
    super(tokenString);
    this.tokenString = tokenString;
  }

  @Override
  public HttpStatus getStatus() {
    return HttpStatus.NOT_FOUND;
  }

  @Override
  public String getTitle() {
    return "User token is missing.";
  }

  @Override
  public String getDetail() {
    return "Token '"+tokenString+"' does not exist.";
  }

  @Override
  public String getType() {
    return "https://api.userland.org/errors/user/token/missing";
  }
}
