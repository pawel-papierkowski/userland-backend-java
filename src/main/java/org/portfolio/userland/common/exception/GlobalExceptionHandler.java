package org.portfolio.userland.common.exception;

import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Handles exceptions gracefully, returning standard problem detail (RFC 7807).
 * Specifically handles validation errors.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
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
    problemDetail.setType(URI.create(ex.getType()));
    return problemDetail;
  }

  /**
   * Handle @Valid validation failures - shows errors for all fields that failed verification.
   * Should make life of frontend developer easier.
   * @param ex Exception for validation failures.
   * @param headers – The headers to be written to the response.
   * @param status – The selected response status.
   * @param request – The current request.
   * @return Problem detail.
   */
  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
    ProblemDetail problemDetail = ex.getBody();
    problemDetail.setTitle("Field Validation Failed");
    problemDetail.setDetail("One or more fields failed validation.");
    problemDetail.setType(URI.create("https://api.general.org/errors/validation"));

    Map<String, String> errors = new LinkedHashMap<>();
    for (FieldError error : ex.getBindingResult().getFieldErrors()) {
      errors.put(error.getField(), error.getDefaultMessage());
    }
    problemDetail.setProperty("validation_errors", errors);

    return handleExceptionInternal(ex, problemDetail, headers, status, request);
  }

  /**
   * Handle validation failures for individual controller parameters like @RequestParam, @PathVariable, or @RequestHeader.
   * Should make life of frontend developer easier.
   * @param ex Exception for validation failures.
   * @param headers – The headers to be written to the response.
   * @param status – The selected response status.
   * @param request – The current request.
   * @return Problem detail.
   */
  @Override
  protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
    ProblemDetail problemDetail = ex.getBody();
    problemDetail.setTitle("Request Parameter Validation Failed");
    problemDetail.setDetail("One or more request parameters failed validation.");
    problemDetail.setType(URI.create("https://api.general.org/errors/validation"));

    Map<String, String> errors = new LinkedHashMap<>();
    // Iterate through each invalid method parameter.
    for (ParameterValidationResult result : ex.getParameterValidationResults()) {
      String paramName = result.getMethodParameter().getParameterName();

      for (MessageSourceResolvable resolvable : result.getResolvableErrors()) {
        if (resolvable instanceof FieldError fieldError) {
          // If the parameter is a complex object, we can extract the specific field name.
          errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        } else {
          // For simple parameters like @RequestParam String email.
          errors.put(paramName, resolvable.getDefaultMessage());
        }
      }
    }
    problemDetail.setProperty("validation_errors", errors);

    return handleExceptionInternal(ex, problemDetail, headers, status, request);
  }
}
