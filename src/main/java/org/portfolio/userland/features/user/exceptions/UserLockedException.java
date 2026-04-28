package org.portfolio.userland.features.user.exceptions;

import org.portfolio.userland.common.exception.GeneralException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when user is locked.
 */
public class UserLockedException extends GeneralException {
  private final String email;

  public UserLockedException(String email) {
    super(email);
    this.email = email;
  }

  @Override
  public HttpStatus getStatus() {
    return HttpStatus.CONFLICT;
  }

  @Override
  public String getTitle() {
    return "User is locked.";
  }

  @Override
  public String getDetail() {
    return "User with email '"+email+"' is locked.";
  }

  @Override
  public String getType() {
    return "https://api.userland.org/errors/user/locked";
  }
}
