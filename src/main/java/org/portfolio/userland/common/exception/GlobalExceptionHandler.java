package org.portfolio.userland.common.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.AuthenticationException;
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
 * Handles exceptions gracefully, returning standard problem detail (<b>RFC 7807</b>).
 * <p>Specifically handles:</p>
 * <ul>
 *   <li>exceptions specific for this application (derived from <code>GeneralException</code>)</li>
 *   <li>authentication errors</li>
 *   <li>authorization errors</li>
 *   <li>validation errors</li>
 * </ul>
 * All other (unhandled) exceptions will return <b>500</b>.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
  /**
   * Handle general exceptions specific for this application.
   * @param ex General custom exception.
   * @return Problem detail.
   */
  @ExceptionHandler(GeneralException.class)
  public ResponseEntity<ProblemDetail> handleGeneralException(GeneralException ex, WebRequest request) {
    // Create a standard RFC 7807 response based on data from exception.
    ProblemDetail problemDetail = ProblemDetail.forStatus(ex.getStatus());
    problemDetail.setTitle(ex.getTitle());
    problemDetail.setDetail(ex.getDetail());
    problemDetail.setType(URI.create(ex.getType()));
    // instance is added automatically
    HttpHeaders headers = resolveHeaders(ex);
    return ResponseEntity.of(problemDetail).headers(headers).build();
  }

  /**
   * Handle authentication exceptions.
   * @param ex Exception.
   * @param request Web request.
   * @return Problem detail.
   */
  @ExceptionHandler(AuthenticationException.class)
  public ProblemDetail handleAuthenticationException(AuthenticationException ex, WebRequest request) {
    ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);
    problemDetail.setTitle("Unauthorized");
    problemDetail.setDetail("Authentication is required to access this resource.");
    problemDetail.setType(URI.create("https://api.general.org/errors/unauthorized"));
    // instance is added automatically
    return problemDetail;
  }

  /**
   * Handle authorization exceptions. These can happen if annotations like @PreAuthorize("hasAuthority('ROLE_ADMIN')")
   * prevent access.
   * @param ex Exception.
   * @param request Web request.
   * @return Problem detail.
   */
  @ExceptionHandler({AuthorizationDeniedException.class, AccessDeniedException.class})
  public ProblemDetail handleAccessDenied(Exception ex, WebRequest request) {
    ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.FORBIDDEN);
    problemDetail.setTitle("Forbidden");
    problemDetail.setDetail("You do not have permission to access this resource.");
    problemDetail.setType(URI.create("https://api.general.org/errors/forbidden"));
    // instance is added automatically
    return problemDetail;
  }

  /**
   * Catch all uncaught exceptions to process it properly.
   * @param ex Exception.
   * @return Problem detail.
   */
  @ExceptionHandler(Exception.class)
  public ProblemDetail handleAllUncaughtExceptions(Exception ex, WebRequest request) {
    // Log the actual exception so we can see it in console.
    log.error("Unknown internal server error occurred:", ex);

    // Return a generic 500 error to the frontend to prevent leaking database details and other potential security
    // issues.
    ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
    problemDetail.setTitle("Internal Server Error");
    problemDetail.setDetail("An unexpected error occurred while processing your request.");
    problemDetail.setType(URI.create("https://api.general.org/errors/internal"));
    // instance is added automatically
    return problemDetail;
  }

  //

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
    // instance is added automatically

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
    // instance is added automatically

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

  //

  /**
   * Resolve headers based on custom headers data in exception.
   * @param ex Exception.
   * @return List of HTTP headers.
   */
  private HttpHeaders resolveHeaders(GeneralException ex) {
    if (ex.getCustomHeaders().isEmpty()) return HttpHeaders.EMPTY;
    HttpHeaders headers = new HttpHeaders();
    for (Map.Entry<String, String> entry : ex.getCustomHeaders().entrySet()) {
      headers.add(entry.getKey(), entry.getValue());
    }
    return headers;
  }
}
