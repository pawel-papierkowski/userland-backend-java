package org.portfolio.userland.features.user.exceptions;

import org.portfolio.userland.common.exception.GeneralException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when user cannot be found.
 */
public class UserNotFoundException extends GeneralException {
  private final String email;

  public UserNotFoundException(String email) {
    super(email);
    this.email = email;
  }

  @Override
  public HttpStatus getStatus() {
    return HttpStatus.NOT_FOUND;
  }

  @Override
  public String getTitle() {
    return "User cannot be found.";
  }

  @Override
  public String getDetail() {
    return "User with email '"+email+"' does not exist.";
  }

  @Override
  public String getType() {
    return "https://api.userland.org/errors/user/doesNotExist";
  }
}
