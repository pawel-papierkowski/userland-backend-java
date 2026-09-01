package org.portfolio.userland.common.annotations;

import org.junit.jupiter.api.Test;
import org.portfolio.userland.features.user.controllers.UserController;
import org.portfolio.userland.features.user.dto.standard.password.UserPassResetConfirmReq;
import org.portfolio.userland.features.user.dto.standard.register.UserRegisterReq;
import org.portfolio.userland.features.user.repositories.jwt.UserJwtRepository;
import org.portfolio.userland.features.user.services.standard.*;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Tests only web layer of API handling. This set of tests verify various constraint annotations.
 */
@AnyTest
@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false) // disable web security
@Import({  // because WebMvcTest by default ignores all services
    ProblemDetailService.class
})
public class AnnotationWebTest extends BaseWebTest {
  // We mock services present on UserController because we only care about testing the Controller's @Valid rules.
  @MockitoBean
  private UserRegisterService userRegisterService;
  @MockitoBean
  private UserViewService userViewService;
  @MockitoBean
  private UserEditService userEditService;
  @MockitoBean
  private UserEmailService userEmailService;
  @MockitoBean
  private UserPasswordService userPasswordService;
  @MockitoBean
  private UserDeleteService userDeleteService;

  // Other needed mocks.
  @MockitoBean
  private ConfigService configService;
  @MockitoBean
  private UserJwtRepository userJwtRepository;
  @MockitoBean
  private JwtService jwtService;
  @MockitoBean
  private CustomUserDetailsService customUserDetailsService;
  @MockitoBean
  private PermissionService permissionService;

  //

  @Test
  public void email_invalidEmail() throws Exception {
    // Testing @Email annotation.
    
    // Arrange: Invalid email (missing Top Level Domain like .com).
    UserRegisterReq req = new UserRegisterReq("John Doe", "john.doe@example", "abcABC123!", "en", null, null, false, null, null);

    // Act: Call the API endpoint.
    MvcResult mvcResult = mockMvc.perform(post("/api/users/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andReturn();

    // Assert: API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.BAD_REQUEST.value());

    ProblemDetailBox expectedPdb = new ProblemDetailBox(
        HttpStatus.BAD_REQUEST.value(),
        "Field Validation Failed",
        "One or more fields failed validation.",
        "/api/users/register",
        "https://api.general.org/errors/validation",
        Map.of("validation_errors", Map.of("email", "Must be a valid email address"))
    );
    problemDetailService.assertPd(mvcResult, expectedPdb);
  }

  //

  @Test
  public void validPassword_noPassword() throws Exception {
    // Testing @ValidPassword annotation.
    
    // Arrange: Password violates the @NotBlank constraint, part of @ValidPassword annotation.
    UserRegisterReq req = new UserRegisterReq("John Doe", "john.doe@example.com", null, "en", null, null, false, null, null);

    // Act: Call the API endpoint.
    MvcResult mvcResult = mockMvc.perform(post("/api/users/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andReturn();

    // Assert: API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.BAD_REQUEST.value());

    ProblemDetailBox expectedPdb = new ProblemDetailBox(
        HttpStatus.BAD_REQUEST.value(),
        "Field Validation Failed",
        "One or more fields failed validation.",
        "/api/users/register",
        "https://api.general.org/errors/validation",
        Map.of("validation_errors", Map.of("password", "Password is required"))
    );
    problemDetailService.assertPd(mvcResult, expectedPdb);
  }

  @Test
  public void validPassword_passwordIsTooShort() throws Exception {
    // Testing @ValidPassword annotation.
    
    // Arrange: Password violates the @Size constraint, part of @ValidPassword annotation.
    UserRegisterReq req = new UserRegisterReq("John Doe", "john.doe@example.com", "1aA!", "en", null, null, false, null, null);

    // Act: Call the API endpoint.
    MvcResult mvcResult = mockMvc.perform(post("/api/users/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
            .andReturn();

    // Assert: API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.BAD_REQUEST.value());

    ProblemDetailBox expectedPdb = new ProblemDetailBox(
        HttpStatus.BAD_REQUEST.value(),
        "Field Validation Failed",
        "One or more fields failed validation.",
        "/api/users/register",
        "https://api.general.org/errors/validation",
        Map.of("validation_errors", Map.of("password", "Password must be between 8 and 100 characters"))
    );
    problemDetailService.assertPd(mvcResult, expectedPdb);
  }

  @Test
  public void validPassword_passwordIsInvalid() throws Exception {
    // Testing @ValidPassword annotation.

    // Arrange: Password violates the @Pattern constraint, part of @ValidPassword annotation.
    UserRegisterReq req = new UserRegisterReq("John Doe", "john.doe@example.com", "abc123456", "en", null, null, false, null, null);

    // Act: Call the API endpoint.
    MvcResult mvcResult = mockMvc.perform(post("/api/users/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andReturn();

    // Assert: API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.BAD_REQUEST.value());

    ProblemDetailBox expectedPdb = new ProblemDetailBox(
        HttpStatus.BAD_REQUEST.value(),
        "Field Validation Failed",
        "One or more fields failed validation.",
        "/api/users/register",
        "https://api.general.org/errors/validation",
        Map.of("validation_errors", Map.of("password", "Password must contain at least one uppercase letter, one lowercase letter, one number, and one special character"))
    );
    problemDetailService.assertPd(mvcResult, expectedPdb);
  }

  //

  @Test
  public void validToken_noToken() throws Exception {
    // Testing @ValidToken annotation.

    // Arrange: Token violates the @NotBlank constraint, part of @ValidToken annotation.
    UserPassResetConfirmReq req = new UserPassResetConfirmReq(null, "123abcABC!@#");

    // Act: Call the API endpoint.
    MvcResult mvcResult = mockMvc.perform(patch("/api/users/password/confirm")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andReturn();

    // Assert: API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.BAD_REQUEST.value());

    ProblemDetailBox expectedPdb = new ProblemDetailBox(
        HttpStatus.BAD_REQUEST.value(),
        "Field Validation Failed",
        "One or more fields failed validation.",
        "/api/users/password/confirm",
        "https://api.general.org/errors/validation",
        Map.of("validation_errors", Map.of("token", "Token string is required"))
    );
    problemDetailService.assertPd(mvcResult, expectedPdb);
  }

  @Test
  public void validToken_tokenIsTooShort() throws Exception {
    // Testing @ValidToken annotation.

    // Arrange: Token violates the @Size constraint, part of @ValidToken annotation.
    UserPassResetConfirmReq req = new UserPassResetConfirmReq("nDVAZXAEt1VvrYra", "123abcABC!@#");

    // Act: Call the API endpoint.
    MvcResult mvcResult = mockMvc.perform(patch("/api/users/password/confirm")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andReturn();

    // Assert: API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.BAD_REQUEST.value());

    ProblemDetailBox expectedPdb = new ProblemDetailBox(
        HttpStatus.BAD_REQUEST.value(),
        "Field Validation Failed",
        "One or more fields failed validation.",
        "/api/users/password/confirm",
        "https://api.general.org/errors/validation",
        Map.of("validation_errors", Map.of("token", "Token string must be between 32 and 128 characters"))
    );
    problemDetailService.assertPd(mvcResult, expectedPdb);
  }

  @Test
  public void validToken_tokenHasInvalidCharacters() throws Exception {
    // Testing @ValidToken annotation.

    // Arrange: Token violates the @Pattern constraint, part of @ValidToken annotation.
    UserPassResetConfirmReq req = new UserPassResetConfirmReq("nD%AZ@AEt!Vvr(raDpSCqmqa5UabuXu1", "123abcABC!@#");

    // Act: Call the API endpoint.
    MvcResult mvcResult = mockMvc.perform(patch("/api/users/password/confirm")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andReturn();

    // Assert: API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.BAD_REQUEST.value());

    ProblemDetailBox expectedPdb = new ProblemDetailBox(
        HttpStatus.BAD_REQUEST.value(),
        "Field Validation Failed",
        "One or more fields failed validation.",
        "/api/users/password/confirm",
        "https://api.general.org/errors/validation",
        Map.of("validation_errors", Map.of("token", "Token string must be alphanumeric"))
    );
    problemDetailService.assertPd(mvcResult, expectedPdb);
  }
}
