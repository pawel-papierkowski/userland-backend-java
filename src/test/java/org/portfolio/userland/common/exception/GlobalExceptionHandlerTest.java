package org.portfolio.userland.common.exception;

import org.junit.jupiter.api.Test;
import org.portfolio.userland.system.auth.details.CustomUserDetailsService;
import org.portfolio.userland.system.auth.jwt.JwtService;
import org.portfolio.userland.system.auth.perm.PermissionService;
import org.portfolio.userland.system.config.service.ConfigService;
import org.portfolio.userland.test.base.AnyTest;
import org.portfolio.userland.test.base.BaseWebTest;
import org.portfolio.userland.test.helpers.problemDetail.ProblemDetailBox;
import org.portfolio.userland.test.helpers.problemDetail.ProblemDetailService;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Dedicated test of {@link GlobalExceptionHandler}. Uses a dummy controller that throws each exception type
 * so we can verify the handler converts them to correct RFC 7807 Problem Detail responses.
 */
@AnyTest
@WebMvcTest(DummyExceptionController.class)
@AutoConfigureMockMvc(addFilters = false) // disable web security
@Import({
    ProblemDetailService.class
})
public class GlobalExceptionHandlerTest extends BaseWebTest {
  // Mocks needed by security filter beans (@Service filters component-scanned by @WebMvcTest).
  @MockitoBean
  private JwtService jwtService;
  @MockitoBean
  private CustomUserDetailsService customUserDetailsService;
  @MockitoBean
  private ConfigService configService;
  @MockitoBean
  private PermissionService permissionService;

  // //////////////////////////////////////////////////////////////////////////
  // handleGeneralException

  @Test
  void handleGeneralException_returnsProblemDetail() throws Exception {
    MvcResult mvcResult = mockMvc.perform(get("/dummy/general"))
        .andReturn();

    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.BAD_REQUEST.value());

    ProblemDetailBox expectedPdb = new ProblemDetailBox(
        HttpStatus.BAD_REQUEST.value(),
        "Bad request.",
        "Test detail message.",
        "/dummy/general",
        "https://api.general.org/errors/general/badParams",
        Map.of()
    );
    problemDetailService.assertPd(mvcResult, expectedPdb);
  }

  // //////////////////////////////////////////////////////////////////////////
  // handleGeneralException — errCode property

  @Test
  void handleGeneralExceptionWithErrCode_includesErrCode() throws Exception {
    MvcResult mvcResult = mockMvc.perform(get("/dummy/general-with-errcode"))
        .andReturn();

    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.CONFLICT.value());

    ProblemDetailBox expectedPdb = new ProblemDetailBox(
        HttpStatus.CONFLICT.value(),
        "Wrong password or account.",
        "Wrong password or account was used. Access denied.",
        "/dummy/general-with-errcode",
        "https://api.userland.org/errors/user/wrongPassword",
        Map.of("errCode", "user_0112")
    );
    problemDetailService.assertPd(mvcResult, expectedPdb);
  }

  // //////////////////////////////////////////////////////////////////////////
  // handleGeneralException — custom headers

  @Test
  void handleGeneralExceptionWithCustomHeaders_includesHeaders() throws Exception {
    MvcResult mvcResult = mockMvc.perform(get("/dummy/general-with-headers"))
        .andReturn();

    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.UNAUTHORIZED.value());

    // Verify the custom header is present.
    String wwwAuth = mvcResult.getResponse().getHeader(HttpHeaders.WWW_AUTHENTICATE);
    assertThat(wwwAuth)
        .as("WWW-Authenticate header should be present")
        .isEqualTo("Bearer error=\"invalid_token\"");

    // Verify problem detail body.
    ProblemDetailBox expectedPdb = new ProblemDetailBox(
        HttpStatus.UNAUTHORIZED.value(),
        "Unauthorized",
        "Bearer token is expired, invalid or malformed and cannot be used.",
        "/dummy/general-with-headers",
        "https://api.userland.org/errors/user/malformedToken",
        Map.of("errCode", "jwt_0001")
    );
    problemDetailService.assertPd(mvcResult, expectedPdb);
  }

  // //////////////////////////////////////////////////////////////////////////
  // handleOptimisticLockingFailure

  @Test
  void handleOptimisticLockingFailure_returns409Conflict() throws Exception {
    MvcResult mvcResult = mockMvc.perform(get("/dummy/optimistic-locking"))
        .andReturn();

    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.CONFLICT.value());

    ProblemDetailBox expectedPdb = new ProblemDetailBox(
        HttpStatus.CONFLICT.value(),
        "Data was modified in the meantime.",
        "Data was modified by someone else. Please reload data and try again.",
        "/dummy/optimistic-locking",
        "https://api.general.org/errors/data-stale",
        Map.of()
    );
    problemDetailService.assertPd(mvcResult, expectedPdb);
  }

  // //////////////////////////////////////////////////////////////////////////
  // handleDataIntegrityViolation

  @Test
  void handleDataIntegrityViolation_returns409Conflict() throws Exception {
    MvcResult mvcResult = mockMvc.perform(get("/dummy/data-integrity"))
        .andReturn();

    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.CONFLICT.value());

    ProblemDetailBox expectedPdb = new ProblemDetailBox(
        HttpStatus.CONFLICT.value(),
        "Data conflict.",
        "The request conflicts with the current state of data. Please reload and try again.",
        "/dummy/data-integrity",
        "https://api.general.org/errors/data-conflict",
        Map.of()
    );
    problemDetailService.assertPd(mvcResult, expectedPdb);
  }

  // //////////////////////////////////////////////////////////////////////////
  // handleAllUncaughtExceptions

  @Test
  void handleAllUncaughtExceptions_returns500() throws Exception {
    MvcResult mvcResult = mockMvc.perform(get("/dummy/uncaught"))
        .andReturn();

    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());

    ProblemDetailBox expectedPdb = new ProblemDetailBox(
        HttpStatus.INTERNAL_SERVER_ERROR.value(),
        "Internal Server Error",
        "An unexpected error occurred while processing your request.",
        "/dummy/uncaught",
        "https://api.general.org/errors/internal",
        Map.of()
    );
    problemDetailService.assertPd(mvcResult, expectedPdb);
  }
}
