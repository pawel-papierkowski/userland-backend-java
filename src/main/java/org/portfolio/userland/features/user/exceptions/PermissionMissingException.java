package org.portfolio.userland.features.user.exceptions;

import org.portfolio.userland.common.exception.GeneralException;
import org.portfolio.userland.features.user.constants.UserErrCode;
import org.springframework.http.HttpStatus;

/**
 * Thrown when permission entry is missing.
 */
public class PermissionMissingException extends GeneralException {
  private final String name;

  public PermissionMissingException(String name) {
    super(name);
    this.name = name;
  }

  @Override
  public HttpStatus getStatus() {
    return HttpStatus.NOT_FOUND;
  }

  @Override
  public String getTitle() {
    return "Permission entry is missing.";
  }

  @Override
  public String getDetail() {
    return "Permission entry named '"+name+"' does not exist.";
  }

  @Override
  public String getType() {
    return "https://api.userland.org/errors/permission/missing";
  }

  @Override
  public String getErrCode() {
    return UserErrCode.PERMISSION_MISSING;
  }
}
