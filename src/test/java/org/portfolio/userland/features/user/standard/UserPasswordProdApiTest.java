package org.portfolio.userland.features.user.standard;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.portfolio.userland.features.user.BaseUserTest;
import org.portfolio.userland.features.user.dto.standard.password.UserPassResetLinkReq;
import org.portfolio.userland.features.user.entities.EnUserStatus;
import org.portfolio.userland.features.user.entities.EnUserTokenType;
import org.portfolio.userland.features.user.entities.User;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Integration test for user password reset.
 * Certain errors are different or are not shown at all on production to prevent security issues like email enumeration attacks.
 * These tests ensure errors are hidden correctly on production.
 */
@TestPropertySource(properties = "app.main.build=PROD")
public class UserPasswordProdApiTest extends BaseUserTest {
  @BeforeEach
  public void tearDown() {
    resetDatabase();
  }

  // //////////////////////////////////////////////////////////////////////////

  @Test
  public void errPassResetForUnknownEmail() throws Exception {
    clock.setFixedTime("2026-04-08T10:00:00Z");

    // Arrange: create password reset request.
    UserPassResetLinkReq req = new UserPassResetLinkReq("none@test.com", null);

    // Act: Try to send password reset email to account that do not exist in database.
    MvcResult mvcResult = mockMvc.perform(post("/api/users/password/link")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andReturn();

    // Assert: API Response. Yes, this response is correct.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.NO_CONTENT.value());
    assertThat(mvcResult.getResponse().getContentAsString()).as("Response body should be empty").isEqualTo("");
  }

  @Test
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

    // Assert: API Response. Yes, this response is correct.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.NO_CONTENT.value());
    assertThat(mvcResult.getResponse().getContentAsString()).as("Response body should be empty").isEqualTo("");
  }

  @Test
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

    // Assert: API Response. Yes, this response is correct.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.NO_CONTENT.value());
    assertThat(mvcResult.getResponse().getContentAsString()).as("Response body should be empty").isEqualTo("");
  }

  @Test
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

    // Assert: API Response. Yes, this response is correct.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.NO_CONTENT.value());
    assertThat(mvcResult.getResponse().getContentAsString()).as("Response body should be empty").isEqualTo("");
  }
}
