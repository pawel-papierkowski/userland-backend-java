package org.portfolio.userland.common.services.email.exception;

import org.portfolio.userland.common.exception.GeneralException;
import org.springframework.http.HttpStatus;

/**
 * Thrown when email service fails to send email message.
 */
public class EmailSendFailureException extends GeneralException {
  private final String name;
  private final Exception ex;

  public EmailSendFailureException(String name, Exception ex) {
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
    return "Email failed to send.";
  }

  @Override
  public String getDetail() {
    return "Name: "+name+". Reason: "+ex.getMessage();
  }

  @Override
  public String getType() {
    return "https://api.general.org/errors/email/failedToSend";
  }
}
