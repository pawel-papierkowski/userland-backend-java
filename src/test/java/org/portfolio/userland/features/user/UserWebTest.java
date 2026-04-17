package org.portfolio.userland.features.user;

import org.junit.jupiter.api.Test;
import org.portfolio.userland.features.user.dto.password.UserPassResetReq;
import org.portfolio.userland.features.user.dto.register.UserRegisterReq;
import org.portfolio.userland.features.user.services.UserPasswordService;
import org.portfolio.userland.features.user.services.UserRegisterService;
import org.portfolio.userland.test.base.BaseWebTest;
import org.portfolio.userland.test.helpers.problemDetail.ProblemDetailBox;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Tests only web layer of user handling.
 */
public class UserWebTest extends BaseWebTest {
  // We mock the service because we only care about testing the Controller's @Valid rules.
  @MockitoBean
  private UserRegisterService userRegisterService;
  @MockitoBean
  private UserPasswordService userPasswordService;

  @Test
  public void registrationWhenInvalidEmail() throws Exception {
    // Arrange: invalid email (missing Top Level Domain like .com)
    UserRegisterReq req = new UserRegisterReq("John Doe", "john.doe@example", "abcABC123!", "en");

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
  public void registrationWhenPasswordIsTooWeak() throws Exception {
    // Arrange: password violates the @Size(min = 8, max = 100) constraint
    UserRegisterReq req = new UserRegisterReq("John Doe", "john.doe@example.com", "1aA!", "en");

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
    // Arrange: password violates the @Pattern() constraint
    UserPassResetReq req = new UserPassResetReq("nDVAZXAEt1VvrYrazvxmU8yruiur9cJg", "weakPassword");

    // Act: Call the API endpoint.
    MvcResult mvcResult = mockMvc.perform(post("/api/users/password/reset")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andReturn();

    // Assert API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.BAD_REQUEST.value());

    ProblemDetailBox expectedPdb = new ProblemDetailBox(
        HttpStatus.BAD_REQUEST.value(),
        "Field Validation Failed",
        "One or more fields failed validation.",
        "/api/users/password/reset",
        "https://api.general.org/errors/validation",
        Map.of("validation_errors", Map.of("password", "Password must contain at least one uppercase letter, one lowercase letter, one number, and one special character"))
    );
    problemDetailService.assertPd(mvcResult, expectedPdb);
  }
}
