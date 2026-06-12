package org.portfolio.userland.features.user.standard;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.portfolio.userland.features.user.BaseUserTest;
import org.portfolio.userland.features.user.constants.UserErrCode;
import org.portfolio.userland.features.user.dto.standard.email.UserEmailChangeLinkReq;
import org.portfolio.userland.features.user.entities.EnUserStatus;
import org.portfolio.userland.features.user.entities.EnUserTokenType;
import org.portfolio.userland.features.user.entities.User;
import org.portfolio.userland.test.helpers.context.WithMockCustomUser;
import org.portfolio.userland.test.helpers.problemDetail.ProblemDetailBox;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Integration test for changing email for user account.
 * Certain errors are different or are not shown at all on production to prevent security issues like email enumeration attacks.
 * These tests ensure errors are hidden correctly on production.
 */
@TestPropertySource(properties = "app.main.build=PROD")
public class UserEmailProdApiTest extends BaseUserTest {
  @BeforeEach
  public void tearDown() {
    resetDatabase();
  }

  // //////////////////////////////////////////////////////////////////////////

  @Test
  @WithMockCustomUser
  public void errEmailChangeToSameEmail() throws Exception {
    clock.setFixedTime("2026-04-08T10:00:00Z");

    // Arrange: create active user.
    User expectedUser = userFactory.genUser(EnUserStatus.ACTIVE);
    userRepository.save(expectedUser);

    // Arrange: create email change request. New email is same as email of logged-in user.
    UserEmailChangeLinkReq req = new UserEmailChangeLinkReq(expectedUser.getEmail(), "Password123!", null);

    // Act: Try to send email change link email.
    MvcResult mvcResult = mockMvc.perform(post("/api/users/email/link")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andReturn();

    // Assert: API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.CONFLICT.value());
    ProblemDetailBox expectedPdb = new ProblemDetailBox(
        HttpStatus.CONFLICT.value(),
        "User with given email already exists.",
        "Email 'test@example.com' already exists.",
        "/api/users/email/link",
        "https://api.userland.org/errors/user/email/alreadyExists",
        Map.of("errCode", UserErrCode.EMAIL_IN_USE)
    );
    problemDetailService.assertPd(mvcResult, expectedPdb);
  }

  @Test
  @WithMockCustomUser
  public void errEmailChangeToExistingEmail() throws Exception {
    clock.setFixedTime("2026-04-08T10:00:00Z");

    // Arrange: create active users.
    User expectedUser = userFactory.genUser(EnUserStatus.ACTIVE);
    userRepository.save(expectedUser);
    User otherUser = userFactory.genRandUser(EnUserStatus.ACTIVE);
    userRepository.save(otherUser);

    // Arrange: create email change request. Given email belongs to already existing user.
    UserEmailChangeLinkReq req = new UserEmailChangeLinkReq(otherUser.getEmail(), "Password123!", null);

    // Act: Try to send email change link email.
    MvcResult mvcResult = mockMvc.perform(post("/api/users/email/link")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andReturn();

    // Assert: API Response. Note we pretend everything went fine.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.NO_CONTENT.value());
    assertThat(mvcResult.getResponse().getContentAsString()).as("Response body should be empty").isEqualTo("");
  }

  @Test
  @WithMockCustomUser
  public void errEmailChangeForMissingUser() throws Exception {
    clock.setFixedTime("2026-04-08T10:00:00Z");

    // Arrange: create email change request.
    UserEmailChangeLinkReq req = new UserEmailChangeLinkReq("new.email@example.com", "Password123!", null);

    // Act: Try to send email change email when account do not exist in database.
    MvcResult mvcResult = mockMvc.perform(post("/api/users/email/link")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andReturn();

    // Assert: API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.NOT_FOUND.value());
    ProblemDetailBox expectedPdb = new ProblemDetailBox(
        HttpStatus.NOT_FOUND.value(),
        "User cannot be found.",
        "User with email 'test@example.com' does not exist.",
        "/api/users/email/link",
        "https://api.userland.org/errors/user/doesNotExist",
        Map.of("errCode", UserErrCode.NOT_FOUND)
    );
    problemDetailService.assertPd(mvcResult, expectedPdb);
  }

  @Test
  @WithMockCustomUser
  public void errEmailChangeForPendingUser() throws Exception {
    clock.setFixedTime("2026-04-08T10:00:00Z");

    // Arrange: create pending user.
    User expectedUser = userFactory.genUser(EnUserStatus.PENDING);
    userRepository.save(expectedUser);

    // Arrange: create email change request.
    UserEmailChangeLinkReq req = new UserEmailChangeLinkReq("new.email@example.com", "Password123!", null);

    // Act: Try to send password reset email.
    MvcResult mvcResult = mockMvc.perform(post("/api/users/email/link")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andReturn();

    // Assert: API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.CONFLICT.value());
    ProblemDetailBox expectedPdb = new ProblemDetailBox(
        HttpStatus.CONFLICT.value(),
        "User has invalid status.",
        "User with email 'test@example.com' must have valid status.",
        "/api/users/email/link",
        "https://api.userland.org/errors/user/invalidStatus",
        Map.of("errCode", UserErrCode.INVALID_STATUS)
    );
    problemDetailService.assertPd(mvcResult, expectedPdb);
  }

  @Test
  @WithMockCustomUser
  public void errEmailChangeForLockedUser() throws Exception {
    clock.setFixedTime("2026-04-08T10:00:00Z");

    // Arrange: create locked user.
    User expectedUser = userFactory.genUser(EnUserStatus.ACTIVE);
    expectedUser.setLocked(true);
    userRepository.save(expectedUser);

    // Arrange: create email change request.
    UserEmailChangeLinkReq req = new UserEmailChangeLinkReq("new.email@example.com", "Password123!", null);

    // Act: Try to send password reset email.
    MvcResult mvcResult = mockMvc.perform(post("/api/users/email/link")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andReturn();

    // Assert: API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.CONFLICT.value());
    ProblemDetailBox expectedPdb = new ProblemDetailBox(
        HttpStatus.CONFLICT.value(),
        "User is locked.",
        "User with email 'test@example.com' is locked.",
        "/api/users/email/link",
        "https://api.userland.org/errors/user/locked",
        Map.of("errCode", UserErrCode.LOCKED)
    );
    problemDetailService.assertPd(mvcResult, expectedPdb);
  }

  @Test
  @WithMockCustomUser
  public void errEmailChangeWhenTokenExists() throws Exception {
    clock.setFixedTime("2026-04-08T10:00:00Z");

    // Arrange: create user with email change token already present and valid.
    User expectedUser = userFactory.genUser(EnUserStatus.ACTIVE);
    userTokenFactory.genTokenEntry(expectedUser, EnUserTokenType.EMAIL, null);
    userRepository.save(expectedUser);

    // Arrange: create email change request.
    UserEmailChangeLinkReq req = new UserEmailChangeLinkReq("new.email@example.com", "Password123!", null);

    // Act: Try to send password reset email.
    MvcResult mvcResult = mockMvc.perform(post("/api/users/email/link")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andReturn();

    // Assert: API Response. Note this is same error as when wrong password is given.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.CONFLICT.value());
    ProblemDetailBox expectedPdb = new ProblemDetailBox(
        HttpStatus.CONFLICT.value(),
        "Required token already exists.",
        "Token of type 'EMAIL' already exists and is still valid. You cannot do this action twice in row.",
        "/api/users/email/link",
        "https://api.userland.org/errors/user/token/alreadyExists",
        Map.of("errCode", UserErrCode.TOKEN_ALREADY)
    );
    problemDetailService.assertPd(mvcResult, expectedPdb);
  }
}
