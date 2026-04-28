package org.portfolio.userland.features.user;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.portfolio.userland.features.user.dto.delete.UserDeleteConfirmReq;
import org.portfolio.userland.features.user.dto.delete.UserDeleteLinkReq;
import org.portfolio.userland.features.user.entities.*;
import org.portfolio.userland.features.user.events.UserAccountDeleteConfirmEvent;
import org.portfolio.userland.features.user.events.UserAccountDeleteLinkEvent;
import org.portfolio.userland.test.helpers.problemDetail.ProblemDetailBox;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Integration test for user account deletion.
 */
public class UserDeleteApiTest extends BaseUserTest {
  @AfterEach
  public void tearDown() {
    resetDatabase();
  }

  // //////////////////////////////////////////////////////////////////////////

  @Test
  public void sendAccountDeletionEmail() throws Exception {
    clock.setFixedTime("2026-04-10T10:00:00Z");
    User expectedUser = userFactory.genUser(EnUserStatus.ACTIVE);

    // Arrange: Create active user in database.
    User newUser = userFactory.genUser(EnUserStatus.ACTIVE);
    userRepository.save(newUser);

    clock.setFixedTime("2026-04-11T11:30:00Z");

    // Arrange: Create account deletion link request.
    UserDeleteLinkReq req = new UserDeleteLinkReq(newUser.getEmail(), null);

    // Act: Request password reset link.
    MvcResult mvcResult = mockMvc.perform(post("/api/users/delete/link")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andReturn();

    // Assert API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.OK.value());

    // Prepare expected result.
    userTokenFactory.genTokenEntry(expectedUser, EnUserTokenType.DELETE, null);
    userHistoryFactory.genHistoryEvent(expectedUser, EnUserHistoryWhat.DELETE_REQ);

    AtomicReference<String> accDeleteToken = new AtomicReference<>();

    // Assert that user data is correctly updated.
    transactionTemplate.execute(status -> {
      User actualUser = userRepository.findByEmail("test@example.com").orElseThrow();
      userAssert.assertIt(actualUser, expectedUser);

      accDeleteToken.set(actualUser.getTokens().getFirst().getToken());
      return null;
    });

    // Assert that the correct event was published.
    assertThat(applicationEvents.stream(UserAccountDeleteLinkEvent.class))
        .as("Event is invalid")
        .hasSize(1)
        .first()
        .satisfies(event -> {
          assertThat(event.id()).isGreaterThan(0L);
          assertThat(event.username()).isEqualTo("Jane");
          assertThat(event.email()).isEqualTo("test@example.com");
          assertThat(event.lang()).isEqualTo("en");
          assertThat(event.frontend()).isNull();
          assertThat(event.accountDeleteToken()).isEqualTo(accDeleteToken.get());
          assertThat(event.accountDeleteTokenExpires()).isEqualTo(30L);
        });
  }

  @Test
  public void sendAccountDeletionEmailWhenExpiredToken() throws Exception {
    clock.setFixedTime("2026-04-10T10:00:00Z");
    User expectedUser = userFactory.genUser(EnUserStatus.ACTIVE);

    // Arrange: Create active user in database with account deletion token already present...
    User newUser = userFactory.genUser(EnUserStatus.ACTIVE);
    userTokenFactory.genTokenEntry(newUser, EnUserTokenType.DELETE, null);
    userRepository.save(newUser);

    // ...but this token is already expired!
    clock.setFixedTime("2026-04-11T11:30:00Z");

    // Arrange: Create account deletion link request.
    UserDeleteLinkReq req = new UserDeleteLinkReq(newUser.getEmail(), null);

    // Act: Request password reset link.
    MvcResult mvcResult = mockMvc.perform(post("/api/users/delete/link")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andReturn();

    // Assert API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.OK.value());

    // Prepare expected result.
    userTokenFactory.genTokenEntry(expectedUser, EnUserTokenType.DELETE, null);
    userHistoryFactory.genHistoryEvent(expectedUser, EnUserHistoryWhat.DELETE_REQ);

    AtomicReference<String> accDeleteToken = new AtomicReference<>();

    // Assert that user data is correctly updated.
    transactionTemplate.execute(status -> {
      User actualUser = userRepository.findByEmail("test@example.com").orElseThrow();
      userAssert.assertIt(actualUser, expectedUser);

      accDeleteToken.set(actualUser.getTokens().getFirst().getToken());
      return null;
    });

    // Assert that the correct event was published.
    assertThat(applicationEvents.stream(UserAccountDeleteLinkEvent.class))
        .as("Event is invalid")
        .hasSize(1)
        .first()
        .satisfies(event -> {
          assertThat(event.id()).isGreaterThan(0L);
          assertThat(event.username()).isEqualTo("Jane");
          assertThat(event.email()).isEqualTo("test@example.com");
          assertThat(event.lang()).isEqualTo("en");
          assertThat(event.frontend()).isNull();
          assertThat(event.accountDeleteToken()).isEqualTo(accDeleteToken.get());
          assertThat(event.accountDeleteTokenExpires()).isEqualTo(30L);
        });
  }

  @Test
  public void actuallyDeleteUser() throws Exception {
    clock.setFixedTime("2026-04-10T10:00:00Z");

    // Arrange: Create active user in database in state indicating it requested account deletion.
    User expectedUser = userFactory.genUser(EnUserStatus.ACTIVE);
    UserToken token = userTokenFactory.genTokenEntry(expectedUser, EnUserTokenType.DELETE, null);
    userHistoryFactory.genHistoryEvent(expectedUser, EnUserHistoryWhat.DELETE_REQ);
    userRepository.save(expectedUser);

    clock.setFixedTime("2026-04-10T10:05:00Z");

    // Arrange: Create password reset request.
    UserDeleteConfirmReq req = new UserDeleteConfirmReq(token.getToken());

    // Act: Request password reset.
    MvcResult mvcResult = mockMvc.perform(post("/api/users/delete/confirm")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andReturn();

    // Assert API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.OK.value());

    // Assert that user is gone.
    transactionTemplate.execute(status -> {
      assertThat(userRepository.findByEmail("test@example.com").isPresent()).as("User should be deleted").isFalse();
      return null;
    });

    // Assert that the correct event was published.
    assertThat(applicationEvents.stream(UserAccountDeleteConfirmEvent.class))
        .as("Event is invalid")
        .hasSize(1)
        .first()
        .satisfies(event -> {
          assertThat(event.id()).isGreaterThan(0L);
          assertThat(event.username()).isEqualTo("Jane");
          assertThat(event.email()).isEqualTo("test@example.com");
          assertThat(event.lang()).isEqualTo("en");
        });
  }

  // //////////////////////////////////////////////////////////////////////////
  // FAILURES

  @Test
  @Transactional
  public void errAccDeleteForPendingUser() throws Exception {
    clock.setFixedTime("2026-04-08T10:00:00Z");

    // Arrange: create pending user.
    User expectedUser = userFactory.genUser(EnUserStatus.PENDING);
    userRepository.save(expectedUser);

    // Arrange: create account deletion request.
    UserDeleteLinkReq req = new UserDeleteLinkReq(expectedUser.getEmail(), null);

    // Act: Try to send account deletion email.
    MvcResult mvcResult = mockMvc.perform(post("/api/users/delete/link")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andReturn();

    // Assert API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.CONFLICT.value());

    ProblemDetailBox expectedPdb = new ProblemDetailBox(
        HttpStatus.CONFLICT.value(),
        "User has invalid status.",
        "User with email '"+expectedUser.getEmail()+"' must have valid status.",
        "/api/users/delete/link",
        "https://api.userland.org/errors/user/invalidStatus",
        Map.of()
    );
    problemDetailService.assertPd(mvcResult, expectedPdb);
  }

  @Test
  @Transactional
  public void errAccDeleteForLockedUser() throws Exception {
    clock.setFixedTime("2026-04-08T10:00:00Z");

    // Arrange: create locked user.
    User expectedUser = userFactory.genUser(EnUserStatus.ACTIVE);
    expectedUser.setLocked(true);
    userRepository.save(expectedUser);

    // Arrange: create account deletion request.
    UserDeleteLinkReq req = new UserDeleteLinkReq(expectedUser.getEmail(), null);

    // Act: Try to send account deletion email.
    MvcResult mvcResult = mockMvc.perform(post("/api/users/delete/link")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andReturn();

    // Assert API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.CONFLICT.value());

    ProblemDetailBox expectedPdb = new ProblemDetailBox(
        HttpStatus.CONFLICT.value(),
        "User is locked.",
        "User with email '"+expectedUser.getEmail()+"' is locked.",
        "/api/users/delete/link",
        "https://api.userland.org/errors/user/locked",
        Map.of()
    );
    problemDetailService.assertPd(mvcResult, expectedPdb);
  }

  @Test
  @Transactional
  public void errAccDeleteWhenTokenExists() throws Exception {
    clock.setFixedTime("2026-04-08T10:00:00Z");

    // Arrange: create user with account deletion token already present and valid.
    User expectedUser = userFactory.genUser(EnUserStatus.ACTIVE);
    userTokenFactory.genTokenEntry(expectedUser, EnUserTokenType.DELETE, null);
    userRepository.save(expectedUser);

    // Arrange: create account deletion request.
    UserDeleteLinkReq req = new UserDeleteLinkReq(expectedUser.getEmail(), null);

    // Act: Try to send account deletion email.
    MvcResult mvcResult = mockMvc.perform(post("/api/users/delete/link")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andReturn();

    // Assert API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.CONFLICT.value());

    ProblemDetailBox expectedPdb = new ProblemDetailBox(
        HttpStatus.CONFLICT.value(),
        "Required token already exists.",
        "Token of type 'DELETE' already exists and is still valid. You cannot do this action twice in row.",
        "/api/users/delete/link",
        "https://api.userland.org/errors/user/token/alreadyExists",
        Map.of()
    );
    problemDetailService.assertPd(mvcResult, expectedPdb);
  }

  //

  @Test
  @Transactional
  public void errAccDeleteWithMissingToken() throws Exception {
    clock.setFixedTime("2026-04-08T10:00:00Z");

    // Arrange: create user that requested password reset.
    User expectedUser = userFactory.genUser(EnUserStatus.ACTIVE);
    UserToken token = userTokenFactory.genTokenEntry(expectedUser, EnUserTokenType.DELETE, null);
    userHistoryFactory.genHistoryEvent(expectedUser, EnUserHistoryWhat.DELETE_REQ);
    userRepository.save(expectedUser);

    // Arrange: account deletion request with deliberately invalid token.
    UserDeleteConfirmReq req = new UserDeleteConfirmReq(token.getToken()+"N");

    // Act: Try to delete user.
    MvcResult mvcResult = mockMvc.perform(post("/api/users/delete/confirm")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andReturn();

    // Assert API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.NOT_FOUND.value());

    ProblemDetailBox expectedPdb = new ProblemDetailBox(
        HttpStatus.NOT_FOUND.value(),
        "User token is missing.",
        "Token '"+token.getToken()+"N' does not exist.",
        "/api/users/delete/confirm",
        "https://api.userland.org/errors/user/token/missing",
        Map.of()
    );
    problemDetailService.assertPd(mvcResult, expectedPdb);
  }
}
