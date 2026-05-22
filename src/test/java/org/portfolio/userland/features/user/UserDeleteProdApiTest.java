package org.portfolio.userland.features.user;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.portfolio.userland.features.user.dto.delete.UserDeleteLinkReq;
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
 * Integration test for user account deletion.
 * Certain errors are different or are not shown at all on production to prevent security issues like email enumeration attacks.
 * These tests ensure errors are hidden correctly on production.
 */
@TestPropertySource(properties = "app.main.build=PROD")
public class UserDeleteProdApiTest extends BaseUserTest {
  @AfterEach
  public void tearDown() {
    resetDatabase();
  }

  // //////////////////////////////////////////////////////////////////////////

  @Test
  @WithMockCustomUser
  @Transactional
  public void errAccDeleteForPendingUser() throws Exception {
    clock.setFixedTime("2026-04-08T10:00:00Z");

    // Arrange: create pending user.
    User expectedUser = userFactory.genUser(EnUserStatus.PENDING);
    userRepository.save(expectedUser);

    // Arrange: create account deletion request.
    UserDeleteLinkReq req = new UserDeleteLinkReq("Password123!", null);

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
        "User with email 'test@example.com' must have valid status.",
        "/api/users/delete/link",
        "https://api.userland.org/errors/user/invalidStatus",
        Map.of("errCode", "user_0121")
    );
    problemDetailService.assertPd(mvcResult, expectedPdb);
  }

  @Test
  @WithMockCustomUser
  @Transactional
  public void errAccDeleteForLockedUser() throws Exception {
    clock.setFixedTime("2026-04-08T10:00:00Z");

    // Arrange: create locked user.
    User expectedUser = userFactory.genUser(EnUserStatus.ACTIVE);
    expectedUser.setLocked(true);
    userRepository.save(expectedUser);

    // Arrange: create account deletion request.
    UserDeleteLinkReq req = new UserDeleteLinkReq("Password123!", null);

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
        "User with email 'test@example.com' is locked.",
        "/api/users/delete/link",
        "https://api.userland.org/errors/user/locked",
        Map.of("errCode", "user_0122")
    );
    problemDetailService.assertPd(mvcResult, expectedPdb);
  }

  @Test
  @WithMockCustomUser
  @Transactional
  public void errAccDeleteWhenTokenExists() throws Exception {
    clock.setFixedTime("2026-04-08T10:00:00Z");

    // Arrange: create user with account deletion token already present and valid.
    User expectedUser = userFactory.genUser(EnUserStatus.ACTIVE);
    userTokenFactory.genTokenEntry(expectedUser, EnUserTokenType.DELETE, null);
    userRepository.save(expectedUser);

    // Arrange: create account deletion request.
    UserDeleteLinkReq req = new UserDeleteLinkReq("Password123!", null);

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
        Map.of("errCode", "user_0013")
    );
    problemDetailService.assertPd(mvcResult, expectedPdb);
  }
}
