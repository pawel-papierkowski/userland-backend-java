package org.portfolio.userland.features.user.standard;

import org.junit.jupiter.api.Test;
import org.portfolio.userland.features.user.controllers.UserController;
import org.portfolio.userland.features.user.dto.standard.delete.UserDeleteConfirmReq;
import org.portfolio.userland.features.user.dto.standard.delete.UserDeleteLinkReq;
import org.portfolio.userland.features.user.dto.standard.edit.UserEditReq;
import org.portfolio.userland.features.user.dto.standard.email.UserEmailChangeConfirmReq;
import org.portfolio.userland.features.user.dto.standard.email.UserEmailChangeLinkReq;
import org.portfolio.userland.features.user.dto.standard.password.UserPassResetConfirmReq;
import org.portfolio.userland.features.user.dto.standard.password.UserPassResetLinkReq;
import org.portfolio.userland.features.user.dto.standard.register.UserActivateReq;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/**
 * Tests only web layer of user handling. Checks standard API.
 */
@AnyTest
@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false) // disable web security
@Import({  // because WebMvcTest by default ignores all services
    ProblemDetailService.class
})
public class UserStandardWebTest extends BaseWebTest {
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
  public void userRegister() throws Exception {
    // Arrange: Provide invalid data for user registration.
    UserRegisterReq req = new UserRegisterReq("", "invalidPassword", null, "a", "", "", null, null, null);

    // Act: Call the API endpoint.
    MvcResult mvcResult = mockMvc.perform(post("/api/users/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andReturn();

    // Assert: API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.BAD_REQUEST.value());
    // Assert: Correct problem detail is present.
    ProblemDetailBox expectedPdb = new ProblemDetailBox(
        HttpStatus.BAD_REQUEST.value(),
        "Field Validation Failed",
        "One or more fields failed validation.",
        "/api/users/register",
        "https://api.general.org/errors/validation",
        Map.of("validation_errors", Map.of(
            "email", "Must be a valid email address",
            "lang", "Invalid language code",
            "password", "Password is required",
            "username", "User name is required"
        ))
    );
    problemDetailService.assertPd(mvcResult, expectedPdb);
  }

  @Test
  public void userActivate() throws Exception {
    // Arrange: Provide invalid data for user activation.
    UserActivateReq req = new UserActivateReq("invalidToken", null);

    // Act: Call the API endpoint.
    MvcResult mvcResult = mockMvc.perform(post("/api/users/activate")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andReturn();

    // Assert: API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.BAD_REQUEST.value());
    // Assert: Correct problem detail is present.
    ProblemDetailBox expectedPdb = new ProblemDetailBox(
        HttpStatus.BAD_REQUEST.value(),
        "Field Validation Failed",
        "One or more fields failed validation.",
        "/api/users/activate",
        "https://api.general.org/errors/validation",
        Map.of("validation_errors", Map.of("token", "Token string must be between 32 and 128 characters"))
    );
    problemDetailService.assertPd(mvcResult, expectedPdb);
  }

  @Test
  public void userEdit() throws Exception {
    // Arrange: Provide invalid data for user edit.
    UserEditReq req = new UserEditReq(null, null, null, null);

    // Act: Call the API endpoint.
    MvcResult mvcResult = mockMvc.perform(patch("/api/users/edit")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andReturn();

    // Assert: API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.BAD_REQUEST.value());
    // Assert: Correct problem detail is present.
    ProblemDetailBox expectedPdb = new ProblemDetailBox(
        HttpStatus.BAD_REQUEST.value(),
        "Field Validation Failed",
        "One or more fields failed validation.",
        "/api/users/edit",
        "https://api.general.org/errors/validation",
        Map.of("validation_errors", Map.of(
            "version", "Version cannot be empty"
        ))
    );
    problemDetailService.assertPd(mvcResult, expectedPdb);
  }

  //

  @Test
  public void emailChangeLink() throws Exception {
    // Arrange: Provide invalid email and password.
    UserEmailChangeLinkReq req = new UserEmailChangeLinkReq("invalid@email", "invalidPassword", null);

    // Act: Call the API endpoint.
    MvcResult mvcResult = mockMvc.perform(post("/api/users/email/link")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andReturn();

    // Assert: API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.BAD_REQUEST.value());
    // Assert: Correct problem detail is present.
    ProblemDetailBox expectedPdb = new ProblemDetailBox(
        HttpStatus.BAD_REQUEST.value(),
        "Field Validation Failed",
        "One or more fields failed validation.",
        "/api/users/email/link",
        "https://api.general.org/errors/validation",
        Map.of("validation_errors", Map.of("newEmail", "Must be a valid email address",
            "password", "Password must contain at least one uppercase letter, one lowercase letter, one number, and one special character"))
    );
    problemDetailService.assertPd(mvcResult, expectedPdb);
  }

  @Test
  public void emailChangeConfirm() throws Exception {
    // Arrange: Provide invalid token.
    UserEmailChangeConfirmReq req = new UserEmailChangeConfirmReq("nDVAZXAEt1VvrYra");

    // Act: Call the API endpoint.
    MvcResult mvcResult = mockMvc.perform(patch("/api/users/email/confirm")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andReturn();

    // Assert: API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.BAD_REQUEST.value());

    ProblemDetailBox expectedPdb = new ProblemDetailBox(
        HttpStatus.BAD_REQUEST.value(),
        "Field Validation Failed",
        "One or more fields failed validation.",
        "/api/users/email/confirm",
        "https://api.general.org/errors/validation",
        Map.of("validation_errors", Map.of("token", "Token string must be between 32 and 128 characters"))
    );
    problemDetailService.assertPd(mvcResult, expectedPdb);
  }

  //

  @Test
  public void passResetLink() throws Exception {
    // Arrange: Provide invalid email.
    UserPassResetLinkReq req = new UserPassResetLinkReq("invalid@email", null);

    // Act: Call the API endpoint.
    MvcResult mvcResult = mockMvc.perform(post("/api/users/password/link")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andReturn();

    // Assert: API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.BAD_REQUEST.value());
    // Assert: Correct problem detail is present.
    ProblemDetailBox expectedPdb = new ProblemDetailBox(
        HttpStatus.BAD_REQUEST.value(),
        "Field Validation Failed",
        "One or more fields failed validation.",
        "/api/users/password/link",
        "https://api.general.org/errors/validation",
        Map.of("validation_errors", Map.of("email", "Must be a valid email address"))
    );
    problemDetailService.assertPd(mvcResult, expectedPdb);
  }

  @Test
  public void passResetConfirm() throws Exception {
    // Arrange: Provide weak password and invalid token.
    // Password violates the @Pattern constraint, part of @ValidPassword annotation.
    // Token violates the @Size constraint, part of @ValidToken annotation.
    UserPassResetConfirmReq req = new UserPassResetConfirmReq("nDVAZXAEt1VvrYra", "weakPassword");

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
        Map.of("validation_errors",
            Map.of("password",
                "Password must contain at least one uppercase letter, one lowercase letter, one number, and one special character",
                "token",
                "Token string must be between 32 and 128 characters"))
    );
    problemDetailService.assertPd(mvcResult, expectedPdb);
  }

  //

  @Test
  public void accountDelLink() throws Exception {
    // Arrange: Provide weak password.
    UserDeleteLinkReq req = new UserDeleteLinkReq("weakPassword", null);

    // Act: Call the API endpoint.
    MvcResult mvcResult = mockMvc.perform(post("/api/users/delete/link")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andReturn();

    // Assert: API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.BAD_REQUEST.value());
    // Assert: Correct problem detail is present.
    ProblemDetailBox expectedPdb = new ProblemDetailBox(
        HttpStatus.BAD_REQUEST.value(),
        "Field Validation Failed",
        "One or more fields failed validation.",
        "/api/users/delete/link",
        "https://api.general.org/errors/validation",
        Map.of("validation_errors", Map.of("password", "Password must contain at least one uppercase letter, one lowercase letter, one number, and one special character"))
    );
    problemDetailService.assertPd(mvcResult, expectedPdb);
  }

  @Test
  public void accountDelConfirm() throws Exception {
    // Arrange: Provide invalid token.
    UserDeleteConfirmReq req = new UserDeleteConfirmReq("invalidToken");

    // Act: Call the API endpoint.
    MvcResult mvcResult = mockMvc.perform(delete("/api/users/delete/confirm")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andReturn();

    // Assert: API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.BAD_REQUEST.value());
    // Assert: Correct problem detail is present.
    ProblemDetailBox expectedPdb = new ProblemDetailBox(
        HttpStatus.BAD_REQUEST.value(),
        "Field Validation Failed",
        "One or more fields failed validation.",
        "/api/users/delete/confirm",
        "https://api.general.org/errors/validation",
        Map.of("validation_errors", Map.of("token", "Token string must be between 32 and 128 characters"))
    );
    problemDetailService.assertPd(mvcResult, expectedPdb);
  }
}
