package org.portfolio.userland.system.config.exceptions;

import org.portfolio.userland.common.exception.GeneralException;
import org.springframework.http.HttpStatus;

/**
 * Indicates unknown config variable.
 */
public class ConfigUnknownException extends GeneralException {
  private final String name;

  public ConfigUnknownException(String name) {
    super(name);
    this.name = name;
  }

  @Override
  public HttpStatus getStatus() {
    return HttpStatus.NOT_FOUND;
  }

  @Override
  public String getTitle() {
    return "Config variable cannot be found.";
  }

  @Override
  public String getDetail() {
    return "Configuration variable '"+name+"' does not exist.";
  }

  @Override
  public String getType() {
    return "https://api.userland.org/errors/config/doesNotExist";
  }
}
