package org.portfolio.userland.test.features.user;

import com.google.common.collect.Maps;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.portfolio.userland.common.services.email.data.EmailReq;
import org.portfolio.userland.features.user.data.EnHistoryWhat;
import org.portfolio.userland.features.user.data.EnTokenType;
import org.portfolio.userland.features.user.data.User;
import org.portfolio.userland.features.user.data.UserToken;
import org.portfolio.userland.features.user.dto.password.UserPassResetReq;
import org.portfolio.userland.features.user.dto.password.UserPassSendReq;
import org.portfolio.userland.helpers.problemDetail.ProblemDetailBox;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Integration test for user password reset.
 */
public class UserPasswordApiTest extends BaseUserTest {
  @AfterEach
  public void tearDown() {
    cleanDatabase();
  }

  // //////////////////////////////////////////////////////////////////////////

  @Test
  public void sendPasswordResetEmail() throws Exception {
    clock.setFixedTime("2026-04-10T10:00:00Z");

    // Arrange: Create active user in database.
    User expectedUser = userFactory.genUser();
    userRepository.save(expectedUser);

    clock.setFixedTime("2026-04-11T11:30:00Z");

    // Arrange: Create password reset link request.
    UserPassSendReq req = new UserPassSendReq(expectedUser.getEmail());

    // Act: Request password reset link.
    MvcResult mvcResult = mockMvc.perform(post("/api/users/password/send")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andReturn();

    // Assert API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.OK.value());

    // Prepare expected result.
    userTokenFactory.genTokenEntry(expectedUser, EnTokenType.PASSWORD, null);
    userHistoryFactory.genHistoryEvent(expectedUser, EnHistoryWhat.PASS_RESET_REQ);

    AtomicReference<String> passResetToken = new AtomicReference<>();

    // Assert that user data is correctly updated.
    transactionTemplate.execute(status -> {
      User actualUser = userRepository.findByEmail("test@example.com").orElseThrow();
      userAssert.assertIt(actualUser, expectedUser);

      passResetToken.set(actualUser.getTokens().getFirst().getToken());
      return null;
    });

    // Assert that email (link to password reset page) was sent.
    await().atMost(Duration.ofSeconds(3)).untilAsserted(() -> {
      ArgumentCaptor<EmailReq> captor = ArgumentCaptor.forClass(EmailReq.class);
      verify(emailService, times(1)).sendEmail(captor.capture());

      // Assert that correct email request was sent.
      Map<String, Object> params = Maps.newHashMap();
      params.put("username", "Jane");
      params.put("passwordResetLink", "https://pawel-papierkowski.github.io/frontend-userland-vue/passwordReset?token="+passResetToken.get());
      params.put("passResetTokenExpires", 30L);
      EmailReq expectedEmailReq = new EmailReq(
          null,
          "en",
          "pawel.papierkowski.portfolio@gmail.com",
          List.of("test@example.com"),
          List.of(),
          List.of(),
          "pawel.papierkowski.portfolio@gmail.com",
          "UserLand: password reset",
          "user/passwordReset",
          params,
          null
      );

      EmailReq actualEmailReq = captor.getValue();
      assertThat(actualEmailReq).isEqualTo(expectedEmailReq);
    });
  }

  @Test
  public void actuallyResetPassword() throws Exception {
    clock.setFixedTime("2026-04-10T10:00:00Z");

    // Arrange: Create active user in database in state indicating it requested password reset.
    User expectedUser = userFactory.genUser();
    UserToken token = userTokenFactory.genTokenEntry(expectedUser, EnTokenType.PASSWORD, null);
    userHistoryFactory.genHistoryEvent(expectedUser, EnHistoryWhat.PASS_RESET_REQ);

    userRepository.save(expectedUser);
    String oldPassword = expectedUser.getPassword();

    clock.setFixedTime("2026-04-10T10:05:00Z");

    // Arrange: Create password reset request.
    UserPassResetReq req = new UserPassResetReq(token.getToken(), "N3vP@ssw0rd");

    // Act: Request password reset.
    MvcResult mvcResult = mockMvc.perform(post("/api/users/password/reset")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andReturn();

    // Assert API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.OK.value());

    // Prepare expected result.
    expectedUser.setModifiedAt(clockService.getNowUTC());
    expectedUser.getTokens().clear(); // password reset token should be gone
    userHistoryFactory.genHistoryEvent(expectedUser, EnHistoryWhat.PASS_RESET);

    // Assert that user data is correctly updated.
    transactionTemplate.execute(status -> {
      User actualUser = userRepository.findByEmail("test@example.com").orElseThrow();
      userAssert.assertIt(actualUser, expectedUser);
      // ensure password hash changed
      assertThat(actualUser.getPassword()).as("User password must different").isNotEqualTo(oldPassword);
      return null;
    });

    // Assert that email (password reset confirmation) was sent.
    await().atMost(Duration.ofSeconds(3)).untilAsserted(() -> {
      ArgumentCaptor<EmailReq> captor = ArgumentCaptor.forClass(EmailReq.class);
      verify(emailService, times(1)).sendEmail(captor.capture());

      // Assert that correct email request was sent.
      Map<String, Object> params = Maps.newHashMap();
      params.put("username", "Jane");
      EmailReq expectedEmailReq = new EmailReq(
          null,
          "en",
          "pawel.papierkowski.portfolio@gmail.com",
          List.of("test@example.com"),
          List.of(),
          List.of(),
          "pawel.papierkowski.portfolio@gmail.com",
          "UserLand: password changed",
          "user/passwordConfirm",
          params,
          null
      );

      EmailReq actualEmailReq = captor.getValue();
      assertThat(actualEmailReq).isEqualTo(expectedEmailReq);
    });
  }

  // //////////////////////////////////////////////////////////////////////////
  // FAILURES

  @Test
  @Transactional
  public void errPassResetForPendingUser() throws Exception {
    clock.setFixedTime("2026-04-08T10:00:00Z");

    // Arrange: create pending user.
    User expectedUser = userFactory.genPendingUserWithToken(null);
    userRepository.save(expectedUser);

    // Arrange: create token activation request.
    UserPassSendReq req = new UserPassSendReq(expectedUser.getEmail());

    // Act: Try to send password reset email.
    MvcResult mvcResult = mockMvc.perform(post("/api/users/password/send")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andReturn();

    // Assert API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.CONFLICT.value());

    ProblemDetailBox expectedPdb = new ProblemDetailBox(
        HttpStatus.CONFLICT.value(),
        "User must be active.",
        "User with email '"+expectedUser.getEmail()+"' must be active.",
        "/api/users/password/send",
        "https://api.userland.org/errors/user/mustBeActive",
        Map.of()
    );
    problemDetailService.assertPd(mvcResult, expectedPdb);
  }

  @Test
  @Transactional
  public void errPassResetForLockedUser() throws Exception {
    clock.setFixedTime("2026-04-08T10:00:00Z");

    // Arrange: create locked user.
    User expectedUser = userFactory.genUser();
    expectedUser.setLocked(true);
    userRepository.save(expectedUser);

    // Arrange: create token activation request.
    UserPassSendReq req = new UserPassSendReq(expectedUser.getEmail());

    // Act: Try to send password reset email.
    MvcResult mvcResult = mockMvc.perform(post("/api/users/password/send")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andReturn();

    // Assert API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.CONFLICT.value());

    ProblemDetailBox expectedPdb = new ProblemDetailBox(
        HttpStatus.CONFLICT.value(),
        "User cannot be locked.",
        "User with email '"+expectedUser.getEmail()+"' cannot be locked.",
        "/api/users/password/send",
        "https://api.userland.org/errors/user/cannotBeLocked",
        Map.of()
    );
    problemDetailService.assertPd(mvcResult, expectedPdb);
  }
}
