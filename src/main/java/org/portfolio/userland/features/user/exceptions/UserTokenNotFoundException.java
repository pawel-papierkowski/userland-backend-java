package org.portfolio.userland.features.user.exceptions;

import org.portfolio.userland.common.exception.GeneralException;
import org.portfolio.userland.features.user.constants.UserErrCode;
import org.springframework.http.HttpStatus;

/**
 * Thrown when cannot find token for given user and type.
 */
public class UserTokenNotFoundException extends GeneralException {
  public UserTokenNotFoundException() {
    super("");
  }

  @Override
  public HttpStatus getStatus() {
    return HttpStatus.NOT_FOUND;
  }

  @Override
  public String getTitle() {
    return "User token cannot be found.";
  }

  @Override
  public String getDetail() {
    return "Failed to find desired token.";
  }

  @Override
  public String getType() {
    return "https://api.userland.org/errors/user/token/notFound";
  }

  @Override
  public String getErrCode() {
    return UserErrCode.TOKEN_NOT_FOUND;
  }
}
