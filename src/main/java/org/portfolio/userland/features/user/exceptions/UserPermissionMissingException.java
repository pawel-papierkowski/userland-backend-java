package org.portfolio.userland.features.user.exceptions;

import org.portfolio.userland.common.exception.GeneralException;
import org.portfolio.userland.features.user.constants.UserErrCode;
import org.springframework.http.HttpStatus;

/**
 * Thrown when user permission entry is missing.
 */
public class UserPermissionMissingException extends GeneralException {
  private final Long id;

  public UserPermissionMissingException(Long id) {
    super(id == null ? "unknown id" : id.toString());
    this.id = id;
  }

  @Override
  public HttpStatus getStatus() {
    return HttpStatus.NOT_FOUND;
  }

  @Override
  public String getTitle() {
    return "User permission entry is missing.";
  }

  @Override
  public String getDetail() {
    return "User permission entry id='"+id+"' does not exist.";
  }

  @Override
  public String getType() {
    return "https://api.userland.org/errors/user/permission/missing";
  }

  @Override
  public String getErrCode() {
    return UserErrCode.PERMISSION_USER_MISSING;
  }
}
