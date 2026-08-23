package org.portfolio.userland.features.check.exceptions;

import org.portfolio.userland.common.exception.GeneralException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when work was interrupted before it could finish.
 */
public class CheckInterruptedException extends GeneralException {
  private final InterruptedException ex;

  public CheckInterruptedException(InterruptedException ex) {
    super(ex.getMessage());
    this.ex = ex;
  }

  @Override
  public HttpStatus getStatus() {
    return HttpStatus.INTERNAL_SERVER_ERROR;
  }

  @Override
  public String getTitle() {
    return "Work interrupted.";
  }

  @Override
  public String getDetail() {
    return "Work was interrupted before completion. Reason: " + ex.getMessage();
  }

  @Override
  public String getType() {
    return "https://api.userland.org/errors/check/interrupted";
  }
}
