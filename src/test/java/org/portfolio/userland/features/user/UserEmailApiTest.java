package org.portfolio.userland.features.user;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.portfolio.userland.features.user.dto.email.UserEmailChangeConfirmReq;
import org.portfolio.userland.features.user.dto.email.UserEmailChangeLinkReq;
import org.portfolio.userland.features.user.entities.*;
import org.portfolio.userland.features.user.events.UserEmailChangeConfirmEvent;
import org.portfolio.userland.features.user.events.UserEmailChangeFailEvent;
import org.portfolio.userland.features.user.events.UserEmailChangeRequestEvent;
import org.portfolio.userland.test.helpers.context.WithMockCustomUser;
import org.portfolio.userland.test.helpers.problemDetail.ProblemDetailBox;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Integration test for changing email for user account.
 */
public class UserEmailApiTest extends BaseUserTest {
  @AfterEach
  public void tearDown() {
    resetDatabase();
  }

  // //////////////////////////////////////////////////////////////////////////

  @Test
  @WithMockCustomUser
  public void requestEmailChange() throws Exception {
    clock.setFixedTime("2026-04-10T10:00:00Z");
    User expectedUser = userFactory.genUser(EnUserStatus.ACTIVE);

    // Arrange: Create active user in database.
    User newUser = userFactory.genUser(EnUserStatus.ACTIVE);
    userRepository.save(newUser);

    clock.setFixedTime("2026-04-11T11:30:00Z");

    // Arrange: Create email change request.
    UserEmailChangeLinkReq req = new UserEmailChangeLinkReq("new.email@test.com", "Password123!", null);

    // Act: Request email change link.
    MvcResult mvcResult = mockMvc.perform(post("/api/users/email/link")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andReturn();

    // Assert API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.NO_CONTENT.value());
    assertThat(mvcResult.getResponse().getContentAsString()).as("Response body should be empty").isEqualTo("");

    // Prepare expected result.
    userTokenFactory.genTokenEntry(expectedUser, EnUserTokenType.EMAIL, null, "new.email@test.com");
    userHistoryFactory.genHistoryEvent(expectedUser, EnUserHistoryWhat.EMAIL_CHANGE_REQ, "old: 'test@example.com', new: 'new.email@test.com'");

    AtomicReference<String> emailChangeToken = new AtomicReference<>();

    // Assert that user data is correctly updated.
    transactionTemplate.execute(_ -> {
      User actualUser = userRepository.findByEmail("test@example.com").orElseThrow();
      userAssert.assertIt(actualUser, expectedUser);

      emailChangeToken.set(actualUser.getTokens().getFirst().getToken());
      return null;
    });

    // Assert that the correct event was published.
    assertThat(applicationEvents.stream(UserEmailChangeRequestEvent.class))
        .as("Event is invalid")
        .hasSize(1)
        .first()
        .satisfies(event -> {
          assertThat(event.id()).isGreaterThan(0L);
          assertThat(event.username()).isEqualTo("Jane");
          assertThat(event.email()).isEqualTo("test@example.com");
          assertThat(event.lang()).isEqualTo("en");
          assertThat(event.frontend()).isNull();
          assertThat(event.newEmail()).isEqualTo("new.email@test.com");
          assertThat(event.emailChangeToken()).isEqualTo(emailChangeToken.get());
          assertThat(event.emailChangeTokenExpires()).isEqualTo(30L);
        });
  }

  @Test
  @WithMockCustomUser
  @Transactional
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

    // Assert API Response. Yes, this response is correct. This prevents email enumeration attacks.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.NO_CONTENT.value());
    assertThat(mvcResult.getResponse().getContentAsString()).as("Response body should be empty").isEqualTo("");

    // Assert that user data is unchanged.
    transactionTemplate.execute(_ -> {
      User expectedUser = userFactory.genUser(EnUserStatus.ACTIVE);
      User actualUser = userRepository.findByEmail("test@example.com").orElseThrow();
      userAssert.assertIt(actualUser, expectedUser);
      return null;
    });

    // Assert that this event was NOT published.
    assertThat(applicationEvents.stream(UserEmailChangeRequestEvent.class))
        .as("No event should happen")
        .hasSize(0);

    // Assert that the correct event was published.
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

  @Test
  @WithMockCustomUser
  public void actuallyChangeEmail() throws Exception {
    clock.setFixedTime("2026-04-10T10:00:00Z");

    // Arrange: Create active user in database in state indicating it requested email change.
    User expectedUser = userFactory.genUser(EnUserStatus.ACTIVE);
    UserToken token = userTokenFactory.genTokenEntry(expectedUser, EnUserTokenType.EMAIL, null, "new.email@test.com");
    userHistoryFactory.genHistoryEvent(expectedUser, EnUserHistoryWhat.EMAIL_CHANGE_REQ, "old: 'test@example.com', new: 'new.email@test.com'");
    userRepository.save(expectedUser);

    clock.setFixedTime("2026-04-10T10:05:00Z");

    // Arrange: Create email change request.
    UserEmailChangeConfirmReq req = new UserEmailChangeConfirmReq(token.getToken());

    // Act: Confirm email change.
    MvcResult mvcResult = mockMvc.perform(patch("/api/users/email/confirm")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andReturn();

    // Assert API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.NO_CONTENT.value());
    assertThat(mvcResult.getResponse().getContentAsString()).as("Response body should be empty").isEqualTo("");

    // Prepare expected result.
    expectedUser.setModifiedAt(clockService.getNowUTC());
    expectedUser.setEmail("new.email@test.com");
    expectedUser.getTokens().clear(); // email change token should be gone
    userHistoryFactory.genHistoryEvent(expectedUser, EnUserHistoryWhat.EMAIL_CHANGE, "old: 'test@example.com', new: 'new.email@test.com'");

    // Assert that user data is correctly updated.
    transactionTemplate.execute(_ -> {
      User actualUser = userRepository.findByEmail("new.email@test.com").orElseThrow();
      userAssert.assertIt(actualUser, expectedUser);
      return null;
    });

    // Assert that the correct event was published.
    assertThat(applicationEvents.stream(UserEmailChangeConfirmEvent.class))
        .as("Event is invalid")
        .hasSize(1)
        .first()
        .satisfies(event -> {
          assertThat(event.id()).isGreaterThan(0L);
          assertThat(event.username()).isEqualTo("Jane");
          assertThat(event.email()).isEqualTo("new.email@test.com");
          assertThat(event.lang()).isEqualTo("en");
        });
  }

  // //////////////////////////////////////////////////////////////////////////
  // FAILURES

  @Test
  @WithMockCustomUser
  @Transactional
  public void errEmailChangeForWrongPassword() throws Exception {
    clock.setFixedTime("2026-04-08T10:00:00Z");

    // Arrange: create active user.
    User expectedUser = userFactory.genUser(EnUserStatus.ACTIVE);
    userRepository.save(expectedUser);

    // Arrange: create email change request.
    UserEmailChangeLinkReq req = new UserEmailChangeLinkReq("new.email@test.com", "wr0ngP@ssword", null);

    // Act: Try to send email change link email.
    MvcResult mvcResult = mockMvc.perform(post("/api/users/email/link")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andReturn();

    // Assert API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.CONFLICT.value());
    ProblemDetailBox expectedPdb = new ProblemDetailBox(
        HttpStatus.CONFLICT.value(),
        "Wrong password.",
        "Wrong password was used. Access denied.",
        "/api/users/email/link",
        "https://api.userland.org/errors/user/wrongPassword",
        Map.of()
    );
    problemDetailService.assertPd(mvcResult, expectedPdb);
  }

  @Test
  @WithMockCustomUser
  @Transactional
  public void errEmailChangeToSameEmail() throws Exception {
    clock.setFixedTime("2026-04-08T10:00:00Z");

    // Arrange: create active user.
    User expectedUser = userFactory.genUser(EnUserStatus.ACTIVE);
    userRepository.save(expectedUser);

    // Arrange: create email change request.
    UserEmailChangeLinkReq req = new UserEmailChangeLinkReq("test@example.com", "Password123!", null);

    // Act: Try to send email change link email.
    MvcResult mvcResult = mockMvc.perform(post("/api/users/email/link")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andReturn();

    // Assert API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.CONFLICT.value());
    ProblemDetailBox expectedPdb = new ProblemDetailBox(
        HttpStatus.CONFLICT.value(),
        "User with given email already exists.",
        "Email 'test@example.com' already exists.",
        "/api/users/email/link",
        "https://api.userland.org/errors/user/email/alreadyExists",
        Map.of()
    );
    problemDetailService.assertPd(mvcResult, expectedPdb);
  }

  @Test
  @WithMockCustomUser
  @Transactional
  public void errConfirmEmailChangeForExistingEmail() throws Exception {
    clock.setFixedTime("2026-04-08T10:00:00Z");

    // Arrange: create two active users. One of them is in state indicating email change was requested.
    User otherUser = userFactory.genRandUser(EnUserStatus.ACTIVE);
    userRepository.save(otherUser);
    User expectedUser = userFactory.genUser(EnUserStatus.ACTIVE);
    UserToken token = userTokenFactory.genTokenEntry(expectedUser, EnUserTokenType.EMAIL, null, otherUser.getEmail());
    userHistoryFactory.genHistoryEvent(expectedUser, EnUserHistoryWhat.EMAIL_CHANGE_REQ, "old: 'test@example.com', new: '"+otherUser.getEmail()+"'");
    userRepository.save(expectedUser);

    // Arrange: create email change confirm request.
    UserEmailChangeConfirmReq req = new UserEmailChangeConfirmReq(token.getToken());

    // Act: Try to send email change link email.
    MvcResult mvcResult = mockMvc.perform(patch("/api/users/email/confirm")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andReturn();

    // Assert API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.CONFLICT.value());
    ProblemDetailBox expectedPdb = new ProblemDetailBox(
        HttpStatus.CONFLICT.value(),
        "User with given email already exists.",
        "Email '"+otherUser.getEmail()+"' already exists.",
        "/api/users/email/confirm",
        "https://api.userland.org/errors/user/email/alreadyExists",
        Map.of()
    );
    problemDetailService.assertPd(mvcResult, expectedPdb);
  }
}
