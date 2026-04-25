package org.portfolio.userland.system.lockdown.exceptions;

import org.portfolio.userland.common.exception.GeneralException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when there is general system lockdown.
 */
public class SystemLockdownException extends GeneralException {

  public SystemLockdownException() {
    super("");
  }

  @Override
  public HttpStatus getStatus() {
    return HttpStatus.CONFLICT;
  }

  @Override
  public String getTitle() {
    return "System lockdown is in effect.";
  }

  @Override
  public String getDetail() {
    return "System lockdown is in effect.";
  }

  @Override
  public String getType() {
    return "https://api.userland.org/errors/system/lockdown";
  }
}
