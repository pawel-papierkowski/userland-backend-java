package org.portfolio.userland.features.user.exceptions;

import org.portfolio.userland.common.exception.GeneralException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when user has invalid status.
 */
public class UserInvalidStatusException extends GeneralException {
  private final String email;

  public UserInvalidStatusException(String email) {
    super(email);
    this.email = email;
  }

  @Override
  public HttpStatus getStatus() {
    return HttpStatus.CONFLICT;
  }

  @Override
  public String getTitle() {
    return "User has invalid status.";
  }

  @Override
  public String getDetail() {
    return "User with email '"+email+"' must have valid status.";
  }

  @Override
  public String getType() {
    return "https://api.userland.org/errors/user/invalidStatus";
  }
}
