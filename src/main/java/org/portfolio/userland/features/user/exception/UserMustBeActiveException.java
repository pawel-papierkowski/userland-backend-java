package org.portfolio.userland.features.user.exception;

import org.portfolio.userland.common.exception.GeneralException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when user must be active, but is not.
 */
public class UserMustBeActiveException extends GeneralException {
  private final String email;

  public UserMustBeActiveException(String email) {
    super(email);
    this.email = email;
  }

  @Override
  public HttpStatus getStatus() {
    return HttpStatus.CONFLICT;
  }

  @Override
  public String getTitle() {
    return "User must be active.";
  }

  @Override
  public String getDetail() {
    return "User with email '"+email+"' must be active.";
  }

  @Override
  public String getType() {
    return "https://api.userland.org/errors/user/mustBeActive";
  }
}
