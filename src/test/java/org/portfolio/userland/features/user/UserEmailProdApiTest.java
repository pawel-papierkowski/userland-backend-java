package org.portfolio.userland.features.user;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.portfolio.userland.features.user.dto.email.UserEmailChangeLinkReq;
import org.portfolio.userland.features.user.entities.EnUserStatus;
import org.portfolio.userland.features.user.entities.EnUserTokenType;
import org.portfolio.userland.features.user.entities.User;
import org.portfolio.userland.test.helpers.context.WithMockCustomUser;
import org.portfolio.userland.test.helpers.problemDetail.ProblemDetailBox;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

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
  @AfterEach
  public void tearDown() {
    resetDatabase();
  }

  // //////////////////////////////////////////////////////////////////////////

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

    // Assert API Response. Note this is same error as when wrong password is given.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.CONFLICT.value());
    ProblemDetailBox expectedPdb = new ProblemDetailBox(
        HttpStatus.CONFLICT.value(),
        "Wrong password or account.",
        "Wrong password or account was used. Access denied.",
        "/api/users/email/link",
        "https://api.userland.org/errors/user/wrongPassword",
        Map.of("errCode", "user_0112")
    );
    problemDetailService.assertPd(mvcResult, expectedPdb);
  }

  @Test
  @WithMockCustomUser
  @Transactional
  public void errEmailChangeForUnknownEmail() throws Exception {
    clock.setFixedTime("2026-04-08T10:00:00Z");

    // Arrange: create email change request.
    UserEmailChangeLinkReq req = new UserEmailChangeLinkReq("new.email@example.com", "Password123!", null);

    // Act: Try to send password reset email to account that do not exist in database.
    MvcResult mvcResult = mockMvc.perform(post("/api/users/email/link")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andReturn();

    // Assert API Response. Note this is same error as when wrong password is given.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.CONFLICT.value());
    ProblemDetailBox expectedPdb = new ProblemDetailBox(
        HttpStatus.CONFLICT.value(),
        "Wrong password or account.",
        "Wrong password or account was used. Access denied.",
        "/api/users/email/link",
        "https://api.userland.org/errors/user/wrongPassword",
        Map.of("errCode", "user_0112")
    );
    problemDetailService.assertPd(mvcResult, expectedPdb);
  }

  @Test
  @WithMockCustomUser
  @Transactional
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

    // Assert API Response. Note this is same error as when wrong password is given.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.CONFLICT.value());
    ProblemDetailBox expectedPdb = new ProblemDetailBox(
        HttpStatus.CONFLICT.value(),
        "Wrong password or account.",
        "Wrong password or account was used. Access denied.",
        "/api/users/email/link",
        "https://api.userland.org/errors/user/wrongPassword",
        Map.of("errCode", "user_0112")
    );
    problemDetailService.assertPd(mvcResult, expectedPdb);
  }

  @Test
  @WithMockCustomUser
  @Transactional
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

    // Assert API Response. Note this is same error as when wrong password is given.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.CONFLICT.value());
    ProblemDetailBox expectedPdb = new ProblemDetailBox(
        HttpStatus.CONFLICT.value(),
        "Wrong password or account.",
        "Wrong password or account was used. Access denied.",
        "/api/users/email/link",
        "https://api.userland.org/errors/user/wrongPassword",
        Map.of("errCode", "user_0112")
    );
    problemDetailService.assertPd(mvcResult, expectedPdb);
  }

  @Test
  @WithMockCustomUser
  @Transactional
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

    // Assert API Response. Note this is same error as when wrong password is given.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.CONFLICT.value());
    ProblemDetailBox expectedPdb = new ProblemDetailBox(
        HttpStatus.CONFLICT.value(),
        "Wrong password or account.",
        "Wrong password or account was used. Access denied.",
        "/api/users/email/link",
        "https://api.userland.org/errors/user/wrongPassword",
        Map.of("errCode", "user_0112")
    );
    problemDetailService.assertPd(mvcResult, expectedPdb);
  }
}
