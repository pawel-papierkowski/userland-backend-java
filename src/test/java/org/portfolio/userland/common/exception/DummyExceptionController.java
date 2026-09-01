package org.portfolio.userland.common.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Dummy controller used exclusively by {@link GlobalExceptionHandlerTest}.
 * Each endpoint deliberately throws a different exception type handled by {@link GlobalExceptionHandler}.
 */
@RestController
@RequestMapping("/dummy")
class DummyExceptionController {

  @GetMapping("/general")
  public void general() {
    throw new BadParamsException("Test detail message.");
  }

  @GetMapping("/general-with-errcode")
  public void generalWithErrCode() {
    // UserWrongPasswordException has errCode = "user_0112" and status 409.
    throw new org.portfolio.userland.features.user.exceptions.UserWrongPasswordException();
  }

  @GetMapping("/general-with-headers")
  public void generalWithHeaders() {
    // InvalidBearerTokenException has custom WWW-Authenticate header and errCode.
    throw new org.portfolio.userland.system.auth.jwt.exceptions.InvalidBearerTokenException("test-token");
  }

  @GetMapping("/optimistic-locking")
  public void optimisticLocking() {
    throw new OptimisticLockingFailureException("Simulated concurrent modification");
  }

  @GetMapping("/data-integrity")
  public void dataIntegrity() {
    throw new DataIntegrityViolationException("Simulated unique constraint violation");
  }

  @GetMapping("/uncaught")
  public void uncaught() {
    throw new IllegalStateException("Something unexpected happened");
  }
}
