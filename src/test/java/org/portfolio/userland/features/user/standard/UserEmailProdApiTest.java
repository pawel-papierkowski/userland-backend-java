package org.portfolio.userland.features.user.standard;

import org.junit.jupiter.api.Test;
import org.portfolio.userland.features.user.BaseUserTest;
import org.portfolio.userland.features.user.constants.UserErrCode;
import org.portfolio.userland.features.user.dto.standard.email.UserEmailChangeLinkReq;
import org.portfolio.userland.features.user.entities.EnUserStatus;
import org.portfolio.userland.features.user.entities.EnUserTokenType;
import org.portfolio.userland.features.user.entities.User;
import org.portfolio.userland.features.user.events.UserEmailChangeFailEvent;
import org.portfolio.userland.features.user.events.UserEmailChangeRequestEvent;
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
 * For production there is no difference, because it is impossible to conduct email enumeration attack via email
 * change endpoint. Why? While you provide new email, system will always return success in response, even if email
 * already exists in database.
 */
@TestPropertySource(properties = "app.main.build=PROD")
public class UserEmailProdApiTest extends BaseUserTest {
  @Test
  @WithMockCustomUser
  public void requestEmailChangeForExistingEmail() throws Exception {
    // To prevent email enumeration attack, we need to pretend everything is fine.
    clock.setFixedTime("2026-04-08T10:00:00Z");

    // Arrange: create two active users.
    User someUser = userFactory.genUser(EnUserStatus.ACTIVE);
    userRepository.save(someUser);
    User otherUser = userFactory.genRandUser(EnUserStatus.ACTIVE);
    userRepository.save(otherUser);

    // Arrange: create email change request.
    UserEmailChangeLinkReq req = new UserEmailChangeLinkReq(otherUser.getEmail(), "Password123!", null);

    // Act: Try to send email change link email.
    MvcResult mvcResult = mockMvc.perform(post("/api/users/email/link")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andReturn();

    // Assert: API Response. Yes, this response is correct. This prevents email enumeration attacks.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.NO_CONTENT.value());
    assertThat(mvcResult.getResponse().getContentAsString()).as("Response body should be empty").isEqualTo("");

    // Assert: User data is unchanged.
    transactionTemplate.execute(_ -> {
      User expectedUser = userFactory.genUser(EnUserStatus.ACTIVE);
      User actualUser = userRepository.findByEmail("test@example.com").orElseThrow();
      userAssert.assertIt(actualUser, expectedUser);
      return null;
    });

    // Assert: Email change request event was NOT published.
    assertThat(applicationEvents.stream(UserEmailChangeRequestEvent.class))
        .as("No event should happen")
        .hasSize(0);

    // Assert: the correct event was published.
    assertThat(applicationEvents.stream(UserEmailChangeFailEvent.class))
        .as("Event is invalid")
        .hasSize(1)
        .first()
        .satisfies(event -> {
          assertThat(event.id()).isGreaterThan(0L);
          assertThat(event.username()).isEqualTo("Jane");
          assertThat(event.email()).isEqualTo("test@example.com");
          assertThat(event.lang()).isEqualTo("en");
          assertThat(event.frontend()).isNull();
          assertThat(event.newEmail()).isEqualTo(otherUser.getEmail());
        });
  }

  // //////////////////////////////////////////////////////////////////////////
  // FAILURES

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

    // Act: Try to send email change email.
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

    // Act: Try to send email change email.
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

    // Act: Try to send email change email.
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
