package org.portfolio.userland.system.exceptions;

import org.apache.commons.lang3.StringUtils;
import org.portfolio.userland.common.exception.GeneralException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when there is general system lockdown and user is not exempt.
 */
public class UserLockdownException extends GeneralException {
  private final String email;

  public UserLockdownException(String email) {
    super(email);
    this.email = email;
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
    if (StringUtils.isEmpty(email)) return "User cannot access endpoint: lockdown.";
    return "User with email '"+email+"' cannot access endpoint: lockdown.";
  }

  @Override
  public String getType() {
    return "https://api.userland.org/errors/user/lockdown";
  }
}
