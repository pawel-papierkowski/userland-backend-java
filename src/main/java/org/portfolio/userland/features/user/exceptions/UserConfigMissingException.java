package org.portfolio.userland.features.user.exceptions;

import org.portfolio.userland.common.exception.GeneralException;
import org.portfolio.userland.features.user.constants.UserErrCode;
import org.springframework.http.HttpStatus;

/**
 * Thrown when user config entry is missing.
 */
public class UserConfigMissingException extends GeneralException {
  private final Long id;

  public UserConfigMissingException(Long id) {
    super(id == null ? "unknown id" : id.toString());
    this.id = id;
  }

  @Override
  public HttpStatus getStatus() {
    return HttpStatus.NOT_FOUND;
  }

  @Override
  public String getTitle() {
    return "User config entry is missing.";
  }

  @Override
  public String getDetail() {
    return "User config entry id='"+id+"' does not exist.";
  }

  @Override
  public String getType() {
    return "https://api.userland.org/errors/user/config/missing";
  }

  @Override
  public String getErrCode() {
    return UserErrCode.CONFIG_MISSING;
  }
}
