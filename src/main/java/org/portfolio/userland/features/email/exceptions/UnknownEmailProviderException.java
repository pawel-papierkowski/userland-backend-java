package org.portfolio.userland.features.email.exceptions;

import org.portfolio.userland.common.exception.GeneralException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when email service encounter unknown provider.
 */
public class UnknownEmailProviderException extends GeneralException {
  private final String provider;

  public UnknownEmailProviderException(String provider) {
    super(provider);
    this.provider = provider;
  }

  @Override
  public HttpStatus getStatus() {
    return HttpStatus.NOT_FOUND;
  }

  @Override
  public String getTitle() {
    return "Unknown email provider.";
  }

  @Override
  public String getDetail() {
    return "Email provider '"+provider+"' does not exist.";
  }

  @Override
  public String getType() {
    return "https://api.general.org/errors/email/unknownProvider";
  }
}
