package org.portfolio.userland.features.email.exceptions;

import jakarta.validation.ConstraintViolation;
import org.portfolio.userland.common.exception.GeneralException;
import org.portfolio.userland.features.email.dto.EmailReq;
import org.springframework.http.HttpStatus;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Thrown when email request fails validation. Used to fail fast on internal misuse (bad request is rejected at
 * enqueue time instead of crashing later during actual sending).
 */
public class InvalidEmailReqException extends GeneralException {
  /** Human-readable list of all violated constraints. */
  private final String violationsDetail;

  /**
   * Creates exception from set of constraint violations.
   * @param violations Violations found during validation of EmailReq.
   */
  public InvalidEmailReqException(Set<ConstraintViolation<EmailReq>> violations) {
    super("Email request failed validation.");
    this.violationsDetail = violations.stream()
        .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
        .sorted()
        .collect(Collectors.joining("; "));
  }

  @Override
  public HttpStatus getStatus() {
    return HttpStatus.BAD_REQUEST;
  }

  @Override
  public String getTitle() {
    return "Invalid email request.";
  }

  @Override
  public String getDetail() {
    return "Email request failed validation: " + violationsDetail + ".";
  }

  @Override
  public String getType() {
    return "https://api.general.org/errors/email/invalidRequest";
  }
}
