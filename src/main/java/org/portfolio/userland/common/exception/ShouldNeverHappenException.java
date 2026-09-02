package org.portfolio.userland.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Exception used in "this should never happen" guards. It is just fancier <code>IllegalStateException</code>.
 * <p>If it is thrown, something is very, very wrong.</p>
 */
public class ShouldNeverHappenException extends GeneralException {
  private final String details;

  public ShouldNeverHappenException(String details) {
    super(details);
    this.details = details;
  }

  @Override
  public HttpStatus getStatus() {
    return HttpStatus.INTERNAL_SERVER_ERROR;
  }

  @Override
  public String getTitle() {
    return "This exception should not happen.";
  }

  @Override
  public String getDetail() {
    return details;
  }

  @Override
  public String getType() {
    return "https://api.general.org/errors/shouldNeverHappen";
  }
}
