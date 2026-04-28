package org.portfolio.userland.features.user;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.portfolio.userland.features.user.dto.common.EnFrontendFramework;
import org.portfolio.userland.features.user.dto.password.UserPassResetConfirmReq;
import org.portfolio.userland.features.user.dto.password.UserPassResetLinkReq;
import org.portfolio.userland.features.user.entities.*;
import org.portfolio.userland.features.user.events.UserPasswordResetConfirmEvent;
import org.portfolio.userland.features.user.events.UserPasswordResetLinkEvent;
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
 * Integration test for user password reset.
 */
public class UserPasswordApiTest extends BaseUserTest {
  @AfterEach
  public void tearDown() {
    resetDatabase();
  }

  // //////////////////////////////////////////////////////////////////////////

  @Test
  public void sendPasswordResetEmail() throws Exception {
    clock.setFixedTime("2026-04-10T10:00:00Z");
    User expectedUser = userFactory.genUser(EnUserStatus.ACTIVE);

    // Arrange: Create active user in database.
    User newUser = userFactory.genUser(EnUserStatus.ACTIVE);
    userRepository.save(newUser);

    clock.setFixedTime("2026-04-11T11:30:00Z");

    // Arrange: Create password reset link request.
    UserPassResetLinkReq req = new UserPassResetLinkReq(newUser.getEmail(), null);

    // Act: Request password reset link.
    MvcResult mvcResult = mockMvc.perform(post("/api/users/password/link")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andReturn();

    // Assert API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.NO_CONTENT.value());

    // Prepare expected result.
    userTokenFactory.genTokenEntry(expectedUser, EnUserTokenType.PASSWORD, null);
    userHistoryFactory.genHistoryEvent(expectedUser, EnUserHistoryWhat.PASS_RESET_REQ);

    AtomicReference<String> passResetToken = new AtomicReference<>();

    // Assert that user data is correctly updated.
    transactionTemplate.execute(status -> {
      User actualUser = userRepository.findByEmail("test@example.com").orElseThrow();
      userAssert.assertIt(actualUser, expectedUser);

      passResetToken.set(actualUser.getTokens().getFirst().getToken());
      return null;
    });

    // Assert that the correct event was published.
    assertThat(applicationEvents.stream(UserPasswordResetLinkEvent.class))
        .as("Event is invalid")
        .hasSize(1)
        .first()
        .satisfies(event -> {
          assertThat(event.id()).isGreaterThan(0L);
          assertThat(event.username()).isEqualTo("Jane");
          assertThat(event.email()).isEqualTo("test@example.com");
          assertThat(event.lang()).isEqualTo("en");
          assertThat(event.frontend()).isNull();
          assertThat(event.passwordResetToken()).isEqualTo(passResetToken.get());
          assertThat(event.passwordResetTokenExpires()).isEqualTo(30L);
        });
  }

  @Test
  public void sendPasswordResetEmailWhenExpiredToken() throws Exception {
    clock.setFixedTime("2026-04-10T10:00:00Z");
    User expectedUser = userFactory.genUser(EnUserStatus.ACTIVE);

    // Arrange: Create active user in database with password reset token already present...
    User newUser = userFactory.genUser(EnUserStatus.ACTIVE);
    userTokenFactory.genTokenEntry(newUser, EnUserTokenType.PASSWORD, null);
    userRepository.save(newUser);

    // ...but this token is already expired!
    clock.setFixedTime("2026-04-11T11:30:00Z");

    // Arrange: Create password reset link request.
    UserPassResetLinkReq req = new UserPassResetLinkReq(newUser.getEmail(), EnFrontendFramework.ANGULAR);

    // Act: Request password reset link.
    MvcResult mvcResult = mockMvc.perform(post("/api/users/password/link")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andReturn();

    // Assert API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.NO_CONTENT.value());

    // Prepare expected result.
    userTokenFactory.genTokenEntry(expectedUser, EnUserTokenType.PASSWORD, null);
    userHistoryFactory.genHistoryEvent(expectedUser, EnUserHistoryWhat.PASS_RESET_REQ);

    AtomicReference<String> passResetToken = new AtomicReference<>();

    // Assert that user data is correctly updated.
    transactionTemplate.execute(status -> {
      User actualUser = userRepository.findByEmail("test@example.com").orElseThrow();
      userAssert.assertIt(actualUser, expectedUser);

      passResetToken.set(actualUser.getTokens().getFirst().getToken());
      return null;
    });

    // Assert that the correct event was published.
    assertThat(applicationEvents.stream(UserPasswordResetLinkEvent.class))
        .as("Event is invalid")
        .hasSize(1)
        .first()
        .satisfies(event -> {
          assertThat(event.id()).isGreaterThan(0L);
          assertThat(event.username()).isEqualTo("Jane");
          assertThat(event.email()).isEqualTo("test@example.com");
          assertThat(event.lang()).isEqualTo("en");
          assertThat(event.frontend()).isEqualTo(EnFrontendFramework.ANGULAR);
          assertThat(event.passwordResetToken()).isEqualTo(passResetToken.get());
          assertThat(event.passwordResetTokenExpires()).isEqualTo(30L);
        });
  }

  @Test
  public void actuallyResetPassword() throws Exception {
    clock.setFixedTime("2026-04-10T10:00:00Z");

    // Arrange: Create active user in database in state indicating it requested password reset.
    User expectedUser = userFactory.genUser(EnUserStatus.ACTIVE);
    UserToken token = userTokenFactory.genTokenEntry(expectedUser, EnUserTokenType.PASSWORD, null);
    userHistoryFactory.genHistoryEvent(expectedUser, EnUserHistoryWhat.PASS_RESET_REQ);

    userRepository.save(expectedUser);
    String oldPassword = expectedUser.getPassword();

    clock.setFixedTime("2026-04-10T10:05:00Z");

    // Arrange: Create password reset request.
    UserPassResetConfirmReq req = new UserPassResetConfirmReq(token.getToken(), "N3vP@ssw0rd");

    // Act: Request password reset.
    MvcResult mvcResult = mockMvc.perform(post("/api/users/password/confirm")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andReturn();

    // Assert API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.NO_CONTENT.value());

    // Prepare expected result.
    expectedUser.setModifiedAt(clockService.getNowUTC());
    expectedUser.getTokens().clear(); // password reset token should be gone
    userHistoryFactory.genHistoryEvent(expectedUser, EnUserHistoryWhat.PASS_RESET);

    // Assert that user data is correctly updated.
    transactionTemplate.execute(status -> {
      User actualUser = userRepository.findByEmail("test@example.com").orElseThrow();
      userAssert.assertIt(actualUser, expectedUser);
      // ensure password hash changed
      assertThat(actualUser.getPassword()).as("User password must different").isNotEqualTo(oldPassword);
      return null;
    });

    // Assert that the correct event was published.
    assertThat(applicationEvents.stream(UserPasswordResetConfirmEvent.class))
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
  public void errPassResetForPendingUser() throws Exception {
    clock.setFixedTime("2026-04-08T10:00:00Z");

    // Arrange: create pending user.
    User expectedUser = userFactory.genUser(EnUserStatus.PENDING);
    userRepository.save(expectedUser);

    // Arrange: create password reset request.
    UserPassResetLinkReq req = new UserPassResetLinkReq(expectedUser.getEmail(), null);

    // Act: Try to send password reset email.
    MvcResult mvcResult = mockMvc.perform(post("/api/users/password/link")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andReturn();

    // Assert API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.CONFLICT.value());

    ProblemDetailBox expectedPdb = new ProblemDetailBox(
        HttpStatus.CONFLICT.value(),
        "User has invalid status.",
        "User with email '"+expectedUser.getEmail()+"' must have valid status.",
        "/api/users/password/link",
        "https://api.userland.org/errors/user/invalidStatus",
        Map.of()
    );
    problemDetailService.assertPd(mvcResult, expectedPdb);
  }

  @Test
  @Transactional
  public void errPassResetForLockedUser() throws Exception {
    clock.setFixedTime("2026-04-08T10:00:00Z");

    // Arrange: create locked user.
    User expectedUser = userFactory.genUser(EnUserStatus.ACTIVE);
    expectedUser.setLocked(true);
    userRepository.save(expectedUser);

    // Arrange: create password reset request.
    UserPassResetLinkReq req = new UserPassResetLinkReq(expectedUser.getEmail(), null);

    // Act: Try to send password reset email.
    MvcResult mvcResult = mockMvc.perform(post("/api/users/password/link")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andReturn();

    // Assert API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.CONFLICT.value());

    ProblemDetailBox expectedPdb = new ProblemDetailBox(
        HttpStatus.CONFLICT.value(),
        "User is locked.",
        "User with email '"+expectedUser.getEmail()+"' is locked.",
        "/api/users/password/link",
        "https://api.userland.org/errors/user/locked",
        Map.of()
    );
    problemDetailService.assertPd(mvcResult, expectedPdb);
  }

  @Test
  @Transactional
  public void errPassResetWhenTokenExists() throws Exception {
    clock.setFixedTime("2026-04-08T10:00:00Z");

    // Arrange: create user with password reset token already present and valid.
    User expectedUser = userFactory.genUser(EnUserStatus.ACTIVE);
    userTokenFactory.genTokenEntry(expectedUser, EnUserTokenType.PASSWORD, null);
    userRepository.save(expectedUser);

    // Arrange: create password reset request.
    UserPassResetLinkReq req = new UserPassResetLinkReq(expectedUser.getEmail(), null);

    // Act: Try to send password reset email.
    MvcResult mvcResult = mockMvc.perform(post("/api/users/password/link")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andReturn();

    // Assert API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.CONFLICT.value());

    ProblemDetailBox expectedPdb = new ProblemDetailBox(
        HttpStatus.CONFLICT.value(),
        "Required token already exists.",
        "Token of type 'PASSWORD' already exists and is still valid. You cannot do this action twice in row.",
        "/api/users/password/link",
        "https://api.userland.org/errors/user/token/alreadyExists",
        Map.of()
    );
    problemDetailService.assertPd(mvcResult, expectedPdb);
  }

  //

  @Test
  @Transactional
  public void errPassResetWithMissingToken() throws Exception {
    clock.setFixedTime("2026-04-08T10:00:00Z");

    // Arrange: create user that requested password reset.
    User expectedUser = userFactory.genUser(EnUserStatus.ACTIVE);
    UserToken token = userTokenFactory.genTokenEntry(expectedUser, EnUserTokenType.PASSWORD, null);
    userHistoryFactory.genHistoryEvent(expectedUser, EnUserHistoryWhat.PASS_RESET_REQ);
    userRepository.save(expectedUser);

    // Arrange: password reset request with deliberately invalid token.
    UserPassResetConfirmReq req = new UserPassResetConfirmReq(token.getToken()+"N", "abcABC123!");

    // Act: Try to set new password.
    MvcResult mvcResult = mockMvc.perform(post("/api/users/password/confirm")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andReturn();

    // Assert API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.NOT_FOUND.value());

    ProblemDetailBox expectedPdb = new ProblemDetailBox(
        HttpStatus.NOT_FOUND.value(),
        "User token is missing.",
        "Token '"+token.getToken()+"N' does not exist.",
        "/api/users/password/confirm",
        "https://api.userland.org/errors/user/token/missing",
        Map.of()
    );
    problemDetailService.assertPd(mvcResult, expectedPdb);
  }
}
