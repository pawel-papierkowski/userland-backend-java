package org.portfolio.userland.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
  /**
   * Handle general exceptions specific for this application.
   * @param ex General custom exception.
   * @return Problem detail.
   */
  @ExceptionHandler(GeneralException.class)
  public ProblemDetail handleGeneralException(GeneralException ex) {
    // Create a standard RFC 7807 response based on data from exception.
    ProblemDetail problemDetail = ProblemDetail.forStatus(ex.getStatus());
    // note instance is automatically determined
    problemDetail.setTitle(ex.getTitle());
    problemDetail.setDetail(ex.getDetail());
    problemDetail.setType(URI.create("https://api.userland.com/errors/email-in-use"));
    return problemDetail;
  }

  /**
   * Handle @Valid validation failures.
   * @param ex Exception for validation failures.
   * @return Problem detail.
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ProblemDetail handleValidationExceptions(MethodArgumentNotValidException ex) {
    ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
    problemDetail.setTitle("Validation Failed");
    problemDetail.setDetail("One or more fields failed validation.");
    problemDetail.setType(URI.create("https://api.userland.com/errors/validation"));

    // You can add custom properties to a ProblemDetail object.
    Map<String, String> errors = new HashMap<>();
    for (FieldError error : ex.getBindingResult().getFieldErrors()) {
      errors.put(error.getField(), error.getDefaultMessage());
    }
    problemDetail.setProperty("validation_errors", errors);

    return problemDetail;
  }
}
