package org.portfolio.userland.features.user.exceptions;

import org.apache.commons.lang3.StringUtils;
import org.portfolio.userland.common.exception.GeneralException;
import org.portfolio.userland.features.user.constants.UserErrCode;
import org.springframework.http.HttpStatus;

/**
 * Thrown when user cannot be found.
 */
public class UserNotFoundException extends GeneralException {
  private final Long id;
  private final String email;

  public UserNotFoundException(Long id) {
    super(id == null ? "" : id.toString());
    this.id = id;
    this.email = null;
  }

  public UserNotFoundException(String email) {
    super(email);
    this.id = null;
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
    if (StringUtils.isNotEmpty(email)) return "User with email '"+email+"' does not exist.";
    return "User with id '"+id+"' does not exist.";
  }

  @Override
  public String getType() {
    return "https://api.userland.org/errors/user/doesNotExist";
  }

  @Override
  public String getErrCode() {
    return UserErrCode.NOT_FOUND;
  }
}
