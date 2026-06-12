package org.portfolio.userland.features.user.standard;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.portfolio.userland.features.email.dto.EmailReq;
import org.portfolio.userland.features.user.BaseUserTest;
import org.portfolio.userland.features.user.constants.UserErrCode;
import org.portfolio.userland.features.user.dto.common.EnFrontendFramework;
import org.portfolio.userland.features.user.dto.standard.register.TokenActivateReq;
import org.portfolio.userland.features.user.dto.standard.register.UserRegisterReq;
import org.portfolio.userland.features.user.entities.EnUserStatus;
import org.portfolio.userland.features.user.entities.User;
import org.portfolio.userland.features.user.entities.UserProfile;
import org.portfolio.userland.features.user.events.UserActivatedEvent;
import org.portfolio.userland.features.user.events.UserAlreadyRegisteredEvent;
import org.portfolio.userland.features.user.events.UserRegisteredEvent;
import org.portfolio.userland.system.config.service.ConfigConst;
import org.portfolio.userland.test.helpers.problemDetail.ProblemDetailBox;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Duration;
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
public class UserRegistrationApiTest extends BaseUserTest {
  @BeforeEach
  public void tearDown() {
    resetDatabase();
  }

  // //////////////////////////////////////////////////////////////////////////

  @Test
  public void registerNewUser() throws Exception {
    // Registering new user with minimum data.
    clock.setFixedTime("2026-04-10T10:00:00Z");

    // Arrange: Create the JSON payload.
    UserRegisterReq req = new UserRegisterReq("Jane", "test@example.com", "Password123!", "en", null, null, null, true, null);

    // Act: Call the API endpoint.
    MvcResult mvcResult = mockMvc.perform(post("/api/users/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
            .andReturn();

    // Assert: API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.CREATED.value());
    assertThat(mvcResult.getResponse().getContentAsString()).as("Response body should be empty").isEqualTo("");

    AtomicReference<String> activationToken = new AtomicReference<>();

    // Assert: Database state.
    transactionTemplate.execute(_ -> {
      // Prepare expected result.
      User expectedUser = userFactory.genUser(EnUserStatus.PENDING);
      UserProfile expectedUserProfile = userProfileFactory.genProfile(expectedUser);

      // Assert: User state.
      User actualUser = assertAllUser("test@example.com", expectedUser, expectedUserProfile);
      activationToken.set(actualUser.getTokens().getFirst().getToken());
      return null;
    });

    // Assert that the correct event was published.
    assertThat(applicationEvents.stream(UserRegisteredEvent.class))
        .as("Event is invalid")
        .hasSize(1)
        .first()
        .satisfies(event -> {
          assertThat(event.id()).isGreaterThan(0L);
          assertThat(event.username()).isEqualTo("Jane");
          assertThat(event.email()).isEqualTo("test@example.com");
          assertThat(event.lang()).isEqualTo("en");
          assertThat(event.frontend()).isNull();
          assertThat(event.activationToken()).isEqualTo(activationToken.get());
          assertThat(event.activationTokenExpires()).isEqualTo(24L);
        });
  }

  @Test
  public void registerNewUserWithProfile() throws Exception {
    // Registering new user with profile data included.
    clock.setFixedTime("2026-04-10T10:00:00Z");

    // Arrange: Create the JSON payload.
    UserRegisterReq req = new UserRegisterReq("Jane", "test@example.com", "Password123!", "en", "Jane", "Smith", false, null, null);

    // Act: Call the API endpoint.
    MvcResult mvcResult = mockMvc.perform(post("/api/users/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andReturn();

    // Assert: API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.CREATED.value());
    assertThat(mvcResult.getResponse().getContentAsString()).as("Response body should be empty").isEqualTo("");

    AtomicReference<String> activationToken = new AtomicReference<>();

    // Assert: Database state.
    transactionTemplate.execute(_ -> {
      // Prepare expected result.
      User expectedUser = userFactory.genUser(EnUserStatus.PENDING);
      UserProfile expectedUserProfile = userProfileFactory.genProfile(expectedUser);
      expectedUserProfile.setName("Jane");
      expectedUserProfile.setSurname("Smith");

      // Assert: User state.
      User actualUser = assertAllUser("test@example.com", expectedUser, expectedUserProfile);
      activationToken.set(actualUser.getTokens().getFirst().getToken());
      return null;
    });

    // Assert that the correct event was published.
    assertThat(applicationEvents.stream(UserRegisteredEvent.class))
        .as("Event is invalid")
        .hasSize(1)
        .first()
        .satisfies(event -> {
          assertThat(event.id()).isGreaterThan(0L);
          assertThat(event.username()).isEqualTo("Jane");
          assertThat(event.email()).isEqualTo("test@example.com");
          assertThat(event.lang()).isEqualTo("en");
          assertThat(event.frontend()).isNull();
          assertThat(event.activationToken()).isEqualTo(activationToken.get());
          assertThat(event.activationTokenExpires()).isEqualTo(24L);
        });
  }

  @Test
  public void registerNewUserWithActivation() throws Exception {
    // Registering new user WITH simultaneous activation. Won't work on production.
    clock.setFixedTime("2026-04-10T10:00:00Z");

    // Arrange: Create the JSON payload. Note we already activate that user. This kind of activation works only in test environments.
    UserRegisterReq req = new UserRegisterReq("Jane", "test@example.com", "Password123!", "en", null, null, true, null, null);

    // Act: Call the API endpoint.
    MvcResult mvcResult = mockMvc.perform(post("/api/users/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andReturn();

    // Assert: API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.CREATED.value());
    assertThat(mvcResult.getResponse().getContentAsString()).as("Response body should be empty").isEqualTo("");

    // Assert: Database state.
    transactionTemplate.execute(_ -> {
      // Prepare expected result.
      User expectedUser = userFactory.genUser(EnUserStatus.ACTIVE);
      UserProfile expectedUserProfile = userProfileFactory.genProfile(expectedUser);

      // Assert: User state.
      assertAllUser("test@example.com", expectedUser, expectedUserProfile);
      return null;
    });

    // Assert that the correct event was published.
    assertThat(applicationEvents.stream(UserActivatedEvent.class))
        .as("Event is invalid")
        .hasSize(1)
        .first()
        .satisfies(event -> {
          assertThat(event.id()).isGreaterThan(0L);
          assertThat(event.username()).isEqualTo("Jane");
          assertThat(event.email()).isEqualTo("test@example.com");
          assertThat(event.lang()).isEqualTo("en");
          assertThat(event.frontend()).isNull();
        });
  }

  @Test
  public void registerNewUserInPortfolioMode() throws Exception {
    // Registering new user with minimum data in portfolio mode.
    clock.setFixedTime("2026-04-10T10:00:00Z");
    configService.set(ConfigConst.GENERAL_PORTFOLIO, "1"); // set to portfolio mode

    // Arrange: Create the JSON payload.
    UserRegisterReq req = new UserRegisterReq("Jane", "test@example.com", "Password123!", "en", null, null, null, true, null);

    // Act: Call the API endpoint.
    MvcResult mvcResult = mockMvc.perform(post("/api/users/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andReturn();

    // Assert: API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.CREATED.value());
    assertThat(mvcResult.getResponse().getContentAsString()).as("Response body should be empty").isEqualTo("");

    AtomicReference<String> activationToken = new AtomicReference<>();

    // Assert: Database state.
    transactionTemplate.execute(_ -> {
      // Prepare expected result.
      User expectedUser = userFactory.genUser(EnUserStatus.PENDING, Map.of("role", "admin"));
      UserProfile expectedUserProfile = userProfileFactory.genProfile(expectedUser);

      // Assert: User state.
      User actualUser = assertAllUser("test@example.com", expectedUser, expectedUserProfile);
      activationToken.set(actualUser.getTokens().getFirst().getToken());
      return null;
    });

    // Assert that the correct event was published.
    assertThat(applicationEvents.stream(UserRegisteredEvent.class))
        .as("Event is invalid")
        .hasSize(1)
        .first()
        .satisfies(event -> {
          assertThat(event.id()).isGreaterThan(0L);
          assertThat(event.username()).isEqualTo("Jane");
          assertThat(event.email()).isEqualTo("test@example.com");
          assertThat(event.lang()).isEqualTo("en");
          assertThat(event.frontend()).isNull();
          assertThat(event.activationToken()).isEqualTo(activationToken.get());
          assertThat(event.activationTokenExpires()).isEqualTo(24L);
        });
  }

  //

  @Test
  public void userIsSanitized() throws Exception {
    // Make sure weird username is sanitized.
    clock.setFixedTime("2026-04-10T10:00:00Z");

    // Arrange: Create the JSON payload.
    UserRegisterReq req = new UserRegisterReq("<script>alert('hacked')</script>", "test@example.com", "Password123!", "en", null, null, false, null, EnFrontendFramework.VUE);

    // Act: Call the API endpoint.
    MvcResult mvcResult = mockMvc.perform(post("/api/users/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andReturn();

    // Assert: Database state.
    transactionTemplate.execute(_ -> {
      // Prepare expected result.
      User expectedUser = userFactory.genUser(EnUserStatus.PENDING);
      expectedUser.setUsername("&lt;script&gt;alert(&#39;hacked&#39;)&lt;/script&gt;"); // make sure it is sanitized
      UserProfile expectedUserProfile = userProfileFactory.genProfile(expectedUser);
      // Assert: User state.
      assertAllUser("test@example.com", expectedUser, expectedUserProfile);
      return null;
    });
  }

  @Test
  public void userWithEmailAlreadyExists() throws Exception {
    // We are trying to register user when user with same email already exists in database.
    clock.setFixedTime("2026-04-10T10:00:00Z");
    User expectedUser = userFactory.genUser(EnUserStatus.ACTIVE);

    // Arrange: Create existing user manually.
    User user = userFactory.genUser(EnUserStatus.ACTIVE);
    UserProfile expectedUserProfile = userProfileFactory.genRandProfile(user);
    userProfileRepository.save(expectedUserProfile);

    // Arrange: Create the JSON payload.
    UserRegisterReq req = new UserRegisterReq("testuser", "test@example.com", "SecurePass123!", "en", null, null, false, null, null);

    // Act: Try to register a NEW user with the SAME email via the API.
    MvcResult mvcResult = mockMvc.perform(post("/api/users/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andReturn();

    // Assert: API Response. On production, it will pretend everything is fine.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.CREATED.value());
    assertThat(mvcResult.getResponse().getContentAsString()).as("Response body should be empty").isEqualTo("");

    // Assert that user data is untouched.
    transactionTemplate.execute(_ -> {
      // Assert: User state.
      assertAllUser("test@example.com", expectedUser, expectedUserProfile);
      return null;
    });

    // Assert that the correct event was published.
    assertThat(applicationEvents.stream(UserAlreadyRegisteredEvent.class))
        .as("Event is invalid")
        .hasSize(1)
        .first()
        .satisfies(event -> {
          assertThat(event.id()).isGreaterThan(0L);
          assertThat(event.username()).isEqualTo("Jane");
          assertThat(event.email()).isEqualTo("test@example.com");
          assertThat(event.lang()).isEqualTo("en");
          assertThat(event.frontend()).isNull(); // will use default frontend for www link
        });

    // Assert that email (warning for existing user) was sent.
    await().atMost(Duration.ofSeconds(3)).untilAsserted(() -> {
      ArgumentCaptor<EmailReq> captor = ArgumentCaptor.forClass(EmailReq.class);
      verify(emailService, times(1)).queueEmail(captor.capture());
    });
  }

  //

  @Test
  public void activateUser() throws Exception {
    // We are activating user.
    clock.setFixedTime("2026-04-10T10:00:00Z");
    User expectedUser = userFactory.genUser(EnUserStatus.ACTIVE); // already generate expected user due to date/time

    // Arrange: Create user, profile and activation token.
    User user = userFactory.genUser(EnUserStatus.PENDING);
    UserProfile expectedUserProfile = userProfileFactory.genRandProfile(user);
    userProfileRepository.save(expectedUserProfile);
    String tokenStr = user.getTokens().getFirst().getToken();

    clock.setFixedTime("2026-04-10T10:05:00Z");

    // Arrange: Create token activate request.
    TokenActivateReq req = new TokenActivateReq(tokenStr, EnFrontendFramework.VUE);

    // Act: Try to activate user using valid token.
    MvcResult mvcResult = mockMvc.perform(post("/api/users/activate")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andReturn();

    // Assert: API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.NO_CONTENT.value());
    assertThat(mvcResult.getResponse().getContentAsString()).as("Response body should be empty").isEqualTo("");

    // Assert: Database state.
    transactionTemplate.execute(_ -> {
      // Prepare expected result.
      expectedUser.setModifiedAt(clockService.getNowUTC());
      expectedUser.getHistory().get(1).setCreatedAt(clockService.getNowUTC()); // activate event happened now
      // Assert: User state.
      assertAllUser("test@example.com", expectedUser, expectedUserProfile);
      // Assert that activate token is removed.
      assertThat(userTokenRepository.count()).as("Count of all user tokens is wrong").isEqualTo(0);
      return null;
    });

    // Assert that the correct event was published.
    assertThat(applicationEvents.stream(UserActivatedEvent.class))
        .as("Event is invalid")
        .hasSize(1)
        .first()
        .satisfies(event -> {
          assertThat(event.id()).isGreaterThan(0L);
          assertThat(event.username()).isEqualTo("Jane");
          assertThat(event.email()).isEqualTo("test@example.com");
          assertThat(event.lang()).isEqualTo("en");
          assertThat(event.frontend()).isEqualTo(EnFrontendFramework.VUE);
        });

    // Assert that email (confirmation of account activation) was sent.
    await().atMost(Duration.ofSeconds(3)).untilAsserted(() -> {
      ArgumentCaptor<EmailReq> captor = ArgumentCaptor.forClass(EmailReq.class);
      verify(emailService, times(1)).queueEmail(captor.capture());
    });
  }

  // //////////////////////////////////////////////////////////////////////////
  // FAILURES

  @Test
  public void errMissingToken() throws Exception {
    // We are trying to activate user using nonexistent token.

    // Arrange: Create token activate request.
    String tokenStr = "MISSING_TOKEN___________________";
    TokenActivateReq req = new TokenActivateReq(tokenStr, null); // pad it as it must have at least 32 chars

    // Act: Try to activate user using non-existent token.
    MvcResult mvcResult = mockMvc.perform(post("/api/users/activate")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andReturn();

    // Assert: API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.NOT_FOUND.value());
    // Assert: Content has correct error.
    ProblemDetailBox expectedPdb = new ProblemDetailBox(
        HttpStatus.NOT_FOUND.value(),
        "User token is missing.",
        "Token '"+tokenStr+"' does not exist.",
        "/api/users/activate",
        "https://api.userland.org/errors/user/token/missing",
        Map.of("errCode", UserErrCode.TOKEN_MISSING)
    );
    problemDetailService.assertPd(mvcResult, expectedPdb);
  }

  @Test
  public void errExpiredToken() throws Exception {
    // We are trying to activate user using expired token.
    clock.setFixedTime("2026-04-08T10:00:00Z");

    // Arrange: Create pending user and get activate token.
    User user = userRepository.save(userFactory.genUser(EnUserStatus.PENDING));
    String tokenStr = user.getTokens().getFirst().getToken();

    clock.setFixedTime("2026-04-10T10:00:00Z");

    // Arrange: Create token activate request.
    TokenActivateReq req = new TokenActivateReq(tokenStr, null); // pad it as it must have at least 32 chars

    // Act: Try to activate user using expired token.
    MvcResult mvcResult = mockMvc.perform(post("/api/users/activate")
             .contentType(MediaType.APPLICATION_JSON)
             .content(objectMapper.writeValueAsString(req)))
            .andReturn();

    // Assert: API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.CONFLICT.value());
    // Assert: Content has correct error.
    ProblemDetailBox expectedPdb = new ProblemDetailBox(
        HttpStatus.CONFLICT.value(),
        "User token is expired.",
        "Token '"+tokenStr+"' already expired.",
        "/api/users/activate",
        "https://api.userland.org/errors/user/token/expired",
        Map.of("errCode", UserErrCode.TOKEN_EXPIRED)
    );
    problemDetailService.assertPd(mvcResult, expectedPdb);
  }
}
