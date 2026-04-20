package org.portfolio.userland.features.user.exception;

import org.portfolio.userland.common.exception.GeneralException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when wrong password was given.
 */
public class UserWrongPasswordException extends GeneralException {
  private final String email;

  public UserWrongPasswordException(String email) {
    super(email);
    this.email = email;
  }

  @Override
  public HttpStatus getStatus() {
    return HttpStatus.CONFLICT;
  }

  @Override
  public String getTitle() {
    return "Wrong password.";
  }

  @Override
  public String getDetail() {
    return "Cannot log in as user with email '"+email+"' due to wrong password.";
  }

  @Override
  public String getType() {
    return "https://api.userland.org/errors/user/wrongPassword";
  }
}
