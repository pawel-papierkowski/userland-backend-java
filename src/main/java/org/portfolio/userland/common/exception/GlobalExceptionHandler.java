package org.portfolio.userland.common.exception;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
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
 *   <li>locking failures</li>
 *   <li>database constraint violations</li>
 * </ul>
 * All other (unhandled) exceptions will return <b>500</b>.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
  /**
   * Handle general exceptions specific for this application.
   * @param ex General custom exception.
   * @param request Web request.
   * @return Response entity that contains problem detail.
   */
  @ExceptionHandler(GeneralException.class)
  public ResponseEntity<ProblemDetail> handleGeneralException(GeneralException ex, WebRequest request) {
    // Create a standard RFC 7807 response based on data from exception.
    ProblemDetail problemDetail = ProblemDetail.forStatus(ex.getStatus());
    problemDetail.setTitle(ex.getTitle());
    problemDetail.setDetail(ex.getDetail());
    problemDetail.setType(URI.create(ex.getType()));
    // Instance is added automatically.

    // Add custom properties.
    if (StringUtils.isNotEmpty(ex.getErrCode())) problemDetail.setProperty("errCode", ex.getErrCode());

    HttpHeaders headers = resolveHeaders(ex);
    return ResponseEntity.of(problemDetail).headers(headers).build();
  }

  /**
   * Handle authentication exceptions.
   * <p>Note: this handler is never triggered by application code directly. It is reached in two ways, both via
   * {@link org.springframework.web.servlet.HandlerExceptionResolver} delegation:</p>
   * <ul>
   *   <li>unauthenticated request to any protected endpoint - <code>ProblemDetailAuthenticationEntryPoint</code>
   *       forwards the exception raised by the security filter chains (e.g. <code>InsufficientAuthenticationException</code>) here,</li>
   *   <li>invalid or missing OIDC token on GCP endpoints - oauth2 resource server failures (e.g.
   *       <code>OAuth2AuthenticationException</code>) are routed through the same entry point.</li>
   * </ul>
   * <p>The generic response body is intentional - token validation specifics must not leak to clients.</p>
   * <p>Contract: application code must NEVER throw Spring's <code>AuthenticationException</code> subtypes (like
   * <code>BadCredentialsException</code> or <code>LockedException</code>). Model authentication/authorization
   * failures as {@link GeneralException} derivatives instead (see <code>InvalidBearerTokenException</code>),
   * so they carry proper error codes and headers. If such an exception ever shows up in logs below, it means
   * someone violated this contract.</p>
   * @param ex Exception.
   * @param request Web request.
   * @return Problem detail.
   */
  @ExceptionHandler(AuthenticationException.class)
  public ProblemDetail handleAuthenticationException(AuthenticationException ex, WebRequest request) {
    // Debug level: unauthenticated access is routine traffic, but unexpected AuthenticationException subtypes
    // thrown from within application code would only be visible here. Log type + message only - never log raw
    // tokens or credentials that could be embedded in exception data.
    log.debug("Authentication failed ({}): {}", ex.getClass().getSimpleName(), ex.getMessage());

    ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);
    problemDetail.setTitle("Unauthorized");
    problemDetail.setDetail("Authentication is required to access this resource.");
    problemDetail.setType(URI.create("https://api.general.org/errors/unauthorized"));
    // instance is added automatically
    return problemDetail;
  }

  /**
   * Handle authorization exceptions. Can happen here if access is prevented due to annotations like
   * <code>@PreAuthorize("hasAuthority('ROLE_ADMIN')")</code>.
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
   * Handle optimistic locking failures that happen at flush time. This covers concurrent modification of entities
   * protected by <code>@Version</code> that was not caught by an explicit version check in service layer (for example
   * modification of user profile row by another transaction).
   * @param ex Exception.
   * @param request Web request.
   * @return Problem detail with 409 Conflict status.
   */
  @ExceptionHandler(OptimisticLockingFailureException.class)
  public ProblemDetail handleOptimisticLockingFailure(OptimisticLockingFailureException ex, WebRequest request) {
    // Log at warning level - it is not a server error, but we still want to see contention in logs.
    log.warn("Optimistic locking failure occurred:", ex);

    ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.CONFLICT);
    problemDetail.setTitle("Data was modified in the meantime.");
    problemDetail.setDetail("Data was modified by someone else. Please reload data and try again.");
    problemDetail.setType(URI.create("https://api.general.org/errors/data-stale"));
    // instance is added automatically
    return problemDetail;
  }

  /**
   * Fallback handler for database constraint violations that were not translated by a service layer. Services are
   * expected to catch {@link DataIntegrityViolationException} around risky writes and translate it into a meaningful
   * domain exception (like unique email races); this handler only ensures that any race we missed in the future
   * degrades into a clean <b>409 Conflict</b> instead of an internal server error.
   * @param ex Exception.
   * @param request Web request.
   * @return Problem detail with 409 Conflict status.
   */
  @ExceptionHandler(DataIntegrityViolationException.class)
  public ProblemDetail handleDataIntegrityViolation(DataIntegrityViolationException ex, WebRequest request) {
    // Log at warning level - it is not a server error, but we still want to see unhandled races/constraints in logs.
    log.warn("Unhandled data integrity violation occurred:", ex);

    ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.CONFLICT);
    problemDetail.setTitle("Data conflict.");
    problemDetail.setDetail("The request conflicts with the current state of data. Please reload and try again.");
    problemDetail.setType(URI.create("https://api.general.org/errors/data-conflict"));
    // instance is added automatically
    return problemDetail;
  }

  /**
   * Catch all uncaught exceptions to process it properly.
   * @param ex Exception.
   * @param request Web request.
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
   * @return Response entity.
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
   * @return Response entity.
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
   * @return HTTP headers.
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
