package org.portfolio.userland.features.user;

import org.junit.jupiter.api.Test;
import org.portfolio.userland.features.user.dto.edit.UserEditReq;
import org.portfolio.userland.features.user.dto.password.UserPassResetConfirmReq;
import org.portfolio.userland.features.user.dto.register.UserRegisterReq;
import org.portfolio.userland.features.user.services.*;
import org.portfolio.userland.system.auth.details.CustomUserDetailsService;
import org.portfolio.userland.system.auth.jwt.JwtService;
import org.portfolio.userland.system.auth.perm.PermissionService;
import org.portfolio.userland.system.config.service.ConfigService;
import org.portfolio.userland.test.base.BaseWebTest;
import org.portfolio.userland.test.helpers.problemDetail.ProblemDetailBox;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Tests only web layer of user handling.
 */
public class UserWebTest extends BaseWebTest {
  // We mock services present on UserController because we only care about testing the Controller's @Valid rules.
  @MockitoBean
  private UserRegisterService userRegisterService;
  @MockitoBean
  private UserViewService userViewService;
  @MockitoBean
  private UserEditService userEditService;
  @MockitoBean
  private UserPasswordService userPasswordService;
  @MockitoBean
  private UserDeleteService userDeleteService;

  // Other needed mocks.
  @MockitoBean
  private ConfigService configService;
  @MockitoBean
  private JwtService jwtService;
  @MockitoBean
  private CustomUserDetailsService customUserDetailsService;
  @MockitoBean
  private PermissionService permissionService;

  @Test
  public void registrationWhenInvalidEmail() throws Exception {
    // Arrange: invalid email (missing Top Level Domain like .com)
    UserRegisterReq req = new UserRegisterReq("John Doe", "john.doe@example", "abcABC123!", "en", false, null);

    // Act: Call the API endpoint.
    MvcResult mvcResult = mockMvc.perform(post("/api/users/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andReturn();

    // Assert API Response.
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

  @Test
  public void registrationWhenPasswordIsTooShort() throws Exception {
    // Arrange: password violates the @Size(min = 8, max = 100) constraint
    UserRegisterReq req = new UserRegisterReq("John Doe", "john.doe@example.com", "1aA!", "en", false, null);

    // Act: Call the API endpoint.
    MvcResult mvcResult = mockMvc.perform(post("/api/users/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
            .andReturn();

    // Assert API Response.
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

  //

  @Test
  public void passResetWhenPasswordIsTooWeak() throws Exception {
    // Arrange: password violates the @Pattern constraint
    UserPassResetConfirmReq req = new UserPassResetConfirmReq("nDVAZXAEt1VvrYrazvxmU8yruiur9cJg", "weakPassword");

    // Act: Call the API endpoint.
    MvcResult mvcResult = mockMvc.perform(patch("/api/users/password/confirm")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andReturn();

    // Assert API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.BAD_REQUEST.value());

    ProblemDetailBox expectedPdb = new ProblemDetailBox(
        HttpStatus.BAD_REQUEST.value(),
        "Field Validation Failed",
        "One or more fields failed validation.",
        "/api/users/password/confirm",
        "https://api.general.org/errors/validation",
        Map.of("validation_errors", Map.of("password", "Password must contain at least one uppercase letter, one lowercase letter, one number, and one special character"))
    );
    problemDetailService.assertPd(mvcResult, expectedPdb);
  }

  //

  @Test
  public void editWhenPasswordIsTooWeak() throws Exception {
    // Arrange: password violates the @Pattern constraint
    UserEditReq req = new UserEditReq(null, "weakPassword", null, null, null);

    // Act: Call the API endpoint.
    MvcResult mvcResult = mockMvc.perform(patch("/api/users/edit")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andReturn();

    // Assert API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.BAD_REQUEST.value());

    ProblemDetailBox expectedPdb = new ProblemDetailBox(
        HttpStatus.BAD_REQUEST.value(),
        "Field Validation Failed",
        "One or more fields failed validation.",
        "/api/users/edit",
        "https://api.general.org/errors/validation",
        Map.of("validation_errors", Map.of("password", "Password must contain at least one uppercase letter, one lowercase letter, one number, and one special character"))
    );
    problemDetailService.assertPd(mvcResult, expectedPdb);
  }
}
