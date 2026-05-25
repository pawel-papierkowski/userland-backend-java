package org.portfolio.userland.common.exception;

/**
 * Thrown when more complicated issue with params is present.
 */
public class BadParamsException extends GeneralException {
  private final String message;

  public BadParamsException(String message) {
    super(message);
    this.message = message;
  }

  @Override
  public String getTitle() {
    return "Bad request.";
  }

  @Override
  public String getDetail() {
    return message;
  }

  @Override
  public String getType() {
    return "https://api.general.org/errors/general/badParams";
  }
}
