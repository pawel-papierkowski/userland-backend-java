package org.portfolio.userland.features.user.exceptions;

import org.portfolio.userland.common.exception.GeneralException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when wrong password was given.
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
    return "Cannot login due to wrong password.";
  }

  @Override
  public String getType() {
    return "https://api.userland.org/errors/user/wrongPassword";
  }
}
