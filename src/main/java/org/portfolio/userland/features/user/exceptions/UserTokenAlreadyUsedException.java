package org.portfolio.userland.features.user.exceptions;

import org.portfolio.userland.common.exception.GeneralException;
import org.portfolio.userland.features.user.constants.UserErrCode;
import org.springframework.http.HttpStatus;

/**
 * Thrown when token has already been consumed by another request and thus can't be used again.
 */
public class UserTokenAlreadyUsedException extends GeneralException {
  private final String tokenString;

  public UserTokenAlreadyUsedException(String tokenString) {
    super(tokenString);
    this.tokenString = tokenString;
  }

  @Override
  public HttpStatus getStatus() {
    return HttpStatus.CONFLICT;
  }

  @Override
  public String getTitle() {
    return "User token is already used.";
  }

  @Override
  public String getDetail() {
    return "Token '"+tokenString+"' was already used.";
  }

  @Override
  public String getType() {
    return "https://api.userland.org/errors/user/token/used";
  }

  @Override
  public String getErrCode() {
    return UserErrCode.TOKEN_USED;
  }
}
