package org.portfolio.userland.features.user.exceptions;

import org.portfolio.userland.common.exception.GeneralException;
import org.portfolio.userland.features.user.constants.UserErrCode;
import org.springframework.http.HttpStatus;

/**
 * Thrown when user permission entry is redundant.
 */
public class UserPermissionRedundantException extends GeneralException {
  private final String permission;

  public UserPermissionRedundantException(String permission) {
    super(permission == null ? "unknown permission" : permission);
    this.permission = permission;
  }

  @Override
  public HttpStatus getStatus() {
    return HttpStatus.CONFLICT;
  }

  @Override
  public String getTitle() {
    return "User permission entry is redundant.";
  }

  @Override
  public String getDetail() {
    return "User permission entry '"+permission+"' already exists.";
  }

  @Override
  public String getType() {
    return "https://api.userland.org/errors/user/permission/redundant";
  }

  @Override
  public String getErrCode() {
    return UserErrCode.PERMISSION_USER_REDUNDANT;
  }
}
