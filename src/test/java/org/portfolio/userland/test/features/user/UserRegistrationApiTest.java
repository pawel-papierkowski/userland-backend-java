package org.portfolio.userland.test.features.user;

import com.google.common.collect.Maps;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.portfolio.userland.base.BaseIntegrationTest;
import org.portfolio.userland.common.services.email.EmailService;
import org.portfolio.userland.common.services.email.data.EmailReq;
import org.portfolio.userland.features.user.data.User;
import org.portfolio.userland.features.user.dto.activate.TokenActivateReq;
import org.portfolio.userland.features.user.dto.register.UserRegisterReq;
import org.portfolio.userland.features.user.dto.register.UserRegisterResp;
import org.portfolio.userland.features.user.repositories.UserHistoryRepository;
import org.portfolio.userland.features.user.repositories.UserRepository;
import org.portfolio.userland.features.user.repositories.UserTokenRepository;
import org.portfolio.userland.helpers.asserts.UserAssert;
import org.portfolio.userland.helpers.factories.UserFactory;
import org.portfolio.userland.helpers.problemDetail.ProblemDetailBox;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

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
 * Integration test for user registration.
 */
public class UserRegistrationApiTest extends BaseIntegrationTest {
  @Autowired
  private UserRepository userRepository;
  @Autowired
  private UserTokenRepository userTokenRepository;
  @Autowired
  private UserHistoryRepository userHistoryRepository;

  @Autowired
  private TransactionTemplate transactionTemplate;

  @Autowired
  private UserFactory userFactory;
  @Autowired
  private UserAssert userAssert;

  @MockitoBean
  private EmailService emailService;

  @AfterEach
  public void tearDown() {
    // Clean up the database after every test so tests don't interfere with each other.
    userTokenRepository.deleteAll();
    userHistoryRepository.deleteAll();
    userRepository.deleteAll();
  }

  // //////////////////////////////////////////////////////////////////////////

  @Test
  public void registerNewUser() throws Exception {
    clock.setFixedTime("2026-04-10T10:00:00Z");

    // Arrange: Create the JSON payload.
    UserRegisterReq req = new UserRegisterReq("Jane", "test@example.com", "Password123!", "en");

    // Act: Call the API endpoint.
    MvcResult mvcResult = mockMvc.perform(post("/api/users/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
            .andReturn();

    // Assert API response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.CREATED.value());
    UserRegisterResp actualResp = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), UserRegisterResp.class);
    //UserRegisterResp expectedResp = new UserRegisterResp(1L);
    assertThat(actualResp.id()).as("Response body is wrong").isGreaterThan(0L); // in this way we do not have to know exact id

    AtomicReference<String> activationToken = new AtomicReference<>();

    // Assert database state.
    transactionTemplate.execute(status -> {
      // We wrap it in transactionTemplate because we cannot use @Transactional on this test, as it would break await() later.
      boolean userExists = userRepository.existsByEmail("test@example.com");
      assertThat(userExists).as("User should exist").isTrue();

      // Assert user state.
      User expectedUser = userFactory.genPendingUserWithToken(null);
      User actualUser = userRepository.findByEmail("test@example.com").orElseThrow();
      userAssert.assertIt(actualUser, expectedUser);

      activationToken.set(actualUser.getTokens().getFirst().getToken());
      return null;
    });

    // Assert that email (account registration) was sent.
    await().atMost(Duration.ofSeconds(3)).untilAsserted(() -> {
      ArgumentCaptor<EmailReq> captor = ArgumentCaptor.forClass(EmailReq.class);
      verify(emailService, times(1)).sendEmail(captor.capture());

      // Assert that correct email request was sent.
      Map<String, Object> params = Maps.newHashMap();
      params.put("username", "Jane");
      params.put("activationLink", "https://pawel-papierkowski.github.io/frontend-userland-vue/activate?token="+activationToken.get());
      params.put("linkValidXhours", 24L);
      EmailReq expectedEmailReq = new EmailReq(
          null,
          "en",
          "pawel.papierkowski.portfolio@gmail.com",
          List.of("test@example.com"),
          List.of(),
          List.of(),
          "pawel.papierkowski.portfolio@gmail.com",
          "Account activation in UserLand",
          "user/registration",
          params,
          null
      );

      EmailReq actualEmailReq = captor.getValue();
      assertThat(actualEmailReq).isEqualTo(expectedEmailReq);
    });
  }

  @Test
  public void activateUser() throws Exception {
    clock.setFixedTime("2026-04-10T10:00:00Z");
    User expectedUser = userFactory.genUser(); // already generate expected user due to date/time

    // Arrange: create user and activation token.
    String tokenStr = "Gl7Y3GK9dqFDEjza3KsOU6k0pM9J4Tiq";
    userRepository.save(userFactory.genPendingUserWithToken(tokenStr));

    clock.setFixedTime("2026-04-10T10:05:00Z");

    // Arrange: create token activation request.
    TokenActivateReq req = new TokenActivateReq(tokenStr);

    // Act: Try to activate user using valid token.
    MvcResult mvcResult = mockMvc.perform(post("/api/users/activate")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andReturn();

    // Assert API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.OK.value());

    //entityManager.flush(); // needed or tearDown() will die
    //entityManager.clear();

    // Prepare expected result.
    expectedUser.setModifiedAt(clockService.getNowUTC());
    expectedUser.getHistory().get(1).setCreatedAt(clockService.getNowUTC()); // activation event happened now

    // Assert that user data is correctly updated.
    transactionTemplate.execute(status -> {
      User actualUser = userRepository.findByEmail("test@example.com").orElseThrow();
      userAssert.assertIt(actualUser, expectedUser);
      // Assert that activation token is removed.
      assertThat(userTokenRepository.count()).as("Count of all user tokens is wrong").isEqualTo(0);
      return null;
    });

    // Assert that email (confirmation of account activation) was sent.
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
          "Welcome to UserLand",
          "user/activation",
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
  public void errUserWithEmailAlreadyExists() throws Exception {
    clock.setFixedTime("2026-04-10T10:00:00Z");

    // Arrange: Create existing user manually.
    userRepository.save(userFactory.genPendingUserWithToken(null));

    // Arrange: Create the JSON payload.
    UserRegisterReq req = new UserRegisterReq("testuser", "test@example.com", "SecurePass123!", "en");

    // Act: Try to register a NEW user with the SAME email via the API.
    MvcResult mvcResult = mockMvc.perform(post("/api/users/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
            .andReturn();

    // Assert API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.CONFLICT.value());

    ProblemDetailBox expectedPdb = new ProblemDetailBox(
        HttpStatus.CONFLICT.value(),
        "User with given email already exists.",
        "Email 'test@example.com' already exists.",
        "/api/users/register",
        "https://api.userland.org/errors/user/email/alreadyExists",
        Map.of()
    );
    problemDetailService.assertPd(mvcResult, expectedPdb);
  }

  @Test
  public void errMissingToken() throws Exception {
    // Arrange: create token activation request.
    String tokenStr = "MISSING_TOKEN___________________";
    TokenActivateReq req = new TokenActivateReq(tokenStr); // pad it as it must have at least 32 chars

    // Act: Try to activate user using non-existent token.
    MvcResult mvcResult = mockMvc.perform(post("/api/users/activate")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andReturn();

    // Assert API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.NOT_FOUND.value());

    ProblemDetailBox expectedPdb = new ProblemDetailBox(
        HttpStatus.NOT_FOUND.value(),
        "User token is missing.",
        "Token '"+tokenStr+"' do not exist.",
        "/api/users/activate",
        "https://api.userland.org/errors/user/token/missing",
        Map.of()
    );
    problemDetailService.assertPd(mvcResult, expectedPdb);
  }

  @Test
  @Transactional
  public void errExpiredToken() throws Exception {
    clock.setFixedTime("2026-04-08T10:00:00Z");

    // Arrange: create user and activation token.
    String tokenStr = "REAL_BUT_EXPIRED_TOKEN__________";
    userRepository.save(userFactory.genPendingUserWithToken(tokenStr));

    clock.setFixedTime("2026-04-10T10:00:00Z");

    // Arrange: create token activation request.
    TokenActivateReq req = new TokenActivateReq(tokenStr); // pad it as it must have at least 32 chars

    // Act: Try to activate user using expired token.
    MvcResult mvcResult = mockMvc.perform(post("/api/users/activate")
             .contentType(MediaType.APPLICATION_JSON)
             .content(objectMapper.writeValueAsString(req)))
            .andReturn();

    // Assert API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.CONFLICT.value());

    ProblemDetailBox expectedPdb = new ProblemDetailBox(
        HttpStatus.CONFLICT.value(),
        "User token is expired.",
        "Token '"+tokenStr+"' already expired.",
        "/api/users/activate",
        "https://api.userland.org/errors/user/token/expired",
        Map.of()
    );
    problemDetailService.assertPd(mvcResult, expectedPdb);
  }
}
