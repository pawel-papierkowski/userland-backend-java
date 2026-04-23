package org.portfolio.userland.features.user.exceptions;

import org.portfolio.userland.common.exception.GeneralException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when user cannot be locked, but is.
 */
public class UserCannotBeLockedException extends GeneralException {
  private final String email;

  public UserCannotBeLockedException(String email) {
    super(email);
    this.email = email;
  }

  @Override
  public HttpStatus getStatus() {
    return HttpStatus.CONFLICT;
  }

  @Override
  public String getTitle() {
    return "User cannot be locked.";
  }

  @Override
  public String getDetail() {
    return "User with email '"+email+"' cannot be locked.";
  }

  @Override
  public String getType() {
    return "https://api.userland.org/errors/user/cannotBeLocked";
  }
}
