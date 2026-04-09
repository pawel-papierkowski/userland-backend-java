package org.portfolio.userland.common.exception;

import org.springframework.http.HttpStatus;

/**
 * General custom exception. All business logic exceptions should inherit from this one.
 */
public abstract class GeneralException extends RuntimeException {
  public GeneralException(String message) {
    super(message);
  }

  /**
   * HTTP status to return for this exception. By default, it is 400 Bad Request.
   * @return HTTP status.
   */
  public HttpStatus getStatus() {
    return HttpStatus.BAD_REQUEST;
  }

  /**
   * Title of problem. It should be short summary that is same for all occurrences of same error.
   * Note it will be visible to user and thus should be inter18zed.
   * @return Title.
   */
  public String getTitle() {
    return "General exception occurred.";
  }

  /**
   * Description of problem specific to that occurrence of the error. By default, returns exception message.
   * Note it will be visible to user and thus should be inter18zed.
   * @return Detail.
   */
  public String getDetail() {
    return getMessage();
  }
}
