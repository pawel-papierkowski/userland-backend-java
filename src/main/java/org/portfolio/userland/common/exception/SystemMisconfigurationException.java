package org.portfolio.userland.common.exception;

import org.springframework.http.HttpStatus;

/**
 * If it is thrown, there is some misconfiguration in project.
 */
public class SystemMisconfigurationException extends GeneralException {
  private final String details;

  public SystemMisconfigurationException(String details) {
    super(details);
    this.details = details;
  }

  @Override
  public HttpStatus getStatus() {
    return HttpStatus.INTERNAL_SERVER_ERROR;
  }

  @Override
  public String getTitle() {
    return "System misconfiguration detected.";
  }

  @Override
  public String getDetail() {
    return details;
  }

  @Override
  public String getType() {
    return "https://api.general.org/errors/systemMisconfiguration";
  }
}
