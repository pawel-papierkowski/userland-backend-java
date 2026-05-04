package org.portfolio.userland.features.user.exceptions;

import org.portfolio.userland.common.exception.GeneralException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when wrong password was given.
 * Note: used also when user account does not exist or other issue with account is present. In this way email enumeration
 * attack is prevented, as you cannot determine if account with given email exists.
 */
public class UserWrongPasswordException extends GeneralException {
  public UserWrongPasswordException() {
    super("");
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
    return "Wrong password was used. Access denied.";
  }

  @Override
  public String getType() {
    return "https://api.userland.org/errors/user/wrongPassword";
  }
}
