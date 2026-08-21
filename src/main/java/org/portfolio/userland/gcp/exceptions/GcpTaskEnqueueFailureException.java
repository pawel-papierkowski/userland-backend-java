package org.portfolio.userland.gcp.exceptions;

import org.portfolio.userland.common.exception.GeneralException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when a task fails to be enqueued to GCP Cloud Tasks, either due to payload
 * serialization problems or an API-level failure of the Cloud Tasks client.
 */
public class GcpTaskEnqueueFailureException extends GeneralException {
  private final String name;
  private final Exception ex;

  /**
   * Creates the exception.
   * @param name Name of the method/operation that failed to enqueue the task.
   * @param ex Original exception that caused this failure.
   */
  public GcpTaskEnqueueFailureException(String name, Exception ex) {
    super(name);
    this.name = name;
    this.ex = ex;
  }

  @Override
  public HttpStatus getStatus() {
    return HttpStatus.INTERNAL_SERVER_ERROR;
  }

  @Override
  public String getTitle() {
    return "Failed to enqueue GCP task.";
  }

  @Override
  public String getDetail() {
    return "Operation: "+name+". Reason: "+ex.getMessage();
  }

  @Override
  public String getType() {
    return "https://api.general.org/errors/gcp/taskEnqueueFailed";
  }
}
