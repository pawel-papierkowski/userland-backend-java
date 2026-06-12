package org.portfolio.userland.features.user.exceptions;

import org.portfolio.userland.common.exception.GeneralException;
import org.portfolio.userland.features.user.constants.UserErrCode;
import org.springframework.http.HttpStatus;

/**
 * Thrown when user config entry is redundant.
 */
public class UserConfigRedundantException extends GeneralException {
  private final String config;

  public UserConfigRedundantException(String config) {
    super(config == null ? "unknown config" : config);
    this.config = config;
  }

  @Override
  public HttpStatus getStatus() {
    return HttpStatus.CONFLICT;
  }

  @Override
  public String getTitle() {
    return "User config entry is redundant.";
  }

  @Override
  public String getDetail() {
    return "User config entry '"+config+"' already exists.";
  }

  @Override
  public String getType() {
    return "https://api.userland.org/errors/user/config/redundant";
  }

  @Override
  public String getErrCode() {
    return UserErrCode.CONFIG_REDUNDANT;
  }
}
