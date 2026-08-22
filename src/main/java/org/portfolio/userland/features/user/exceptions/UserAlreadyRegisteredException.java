package org.portfolio.userland.features.user.exceptions;

import org.portfolio.userland.common.exception.GeneralException;
import org.portfolio.userland.features.user.constants.UserErrCode;
import org.springframework.http.HttpStatus;

/**
 * Internal signal that registration lost a race against a concurrent registration with the same email (detected via
 * violation of the unique constraint on <code>users.email</code>). It is caught by the non-transactional orchestrator
 * ({@link org.portfolio.userland.features.user.services.standard.UserRegisterService}), which then runs the graceful
 * "already registered" flow in a fresh transaction - this exception must never escape to controllers.
 */
public class UserAlreadyRegisteredException extends GeneralException {
  private final String email;

  public UserAlreadyRegisteredException(String email) {
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
    return "Email '"+email+"' is already registered.";
  }

  @Override
  public String getType() {
    return "https://api.userland.org/errors/user/alreadyRegistered";
  }

  @Override
  public String getErrCode() {
    return UserErrCode.ALREADY_REGISTERED;
  }
}
