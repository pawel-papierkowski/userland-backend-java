package org.portfolio.userland.features.user.exceptions;

import org.portfolio.userland.common.exception.GeneralException;
import org.portfolio.userland.features.user.entities.EnTokenType;
import org.springframework.http.HttpStatus;

/**
 * Thrown when a user attempts to do action that require token, but token with that type already exists.
 * For example, user requests password reset twice in row.
 */
public class UserTokenAlreadyExistsException extends GeneralException {
  private final EnTokenType tokenType;

  public UserTokenAlreadyExistsException(EnTokenType tokenType) {
    super(tokenType.name());
    this.tokenType = tokenType;
  }

  @Override
  public HttpStatus getStatus() {
    return HttpStatus.CONFLICT;
  }

  @Override
  public String getTitle() {
    return "Required token already exists.";
  }

  @Override
  public String getDetail() {
    return "Token of type '"+tokenType.name()+"' already exists. You cannot do this action twice in row.";
  }

  @Override
  public String getType() {
    return "https://api.userland.org/errors/user/token/alreadyExists";
  }
}
