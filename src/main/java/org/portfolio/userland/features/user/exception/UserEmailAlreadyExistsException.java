package org.portfolio.userland.features.user.exception;

import org.portfolio.userland.common.exception.GeneralException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when a user attempts to register with an email that is already in use.
 */
public class UserEmailAlreadyExistsException extends GeneralException {
  private final String email;

  public UserEmailAlreadyExistsException(String email) {
    super(email);
    this.email = email;
  }

  @Override
  public HttpStatus getStatus() {
    return HttpStatus.CONFLICT;
  }

  @Override
  public String getTitle() {
    return "User with given email already exists.";
  }

  @Override
  public String getDetail() {
    return "Email '"+email+"' already exists.";
  }

  @Override
  public String getType() {
    return "https://api.userland.org/errors/general";
  }
}
