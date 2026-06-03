package org.portfolio.userland.features.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.portfolio.userland.features.email.dto.EmailReq;
import org.portfolio.userland.features.user.dto.standard.register.UserRegisterReq;
import org.portfolio.userland.features.user.entities.EnUserStatus;
import org.portfolio.userland.features.user.entities.User;
import org.portfolio.userland.features.user.entities.UserProfile;
import org.portfolio.userland.features.user.events.UserAlreadyRegisteredEvent;
import org.portfolio.userland.features.user.events.UserRegisteredEvent;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Integration test for user registration on production.
 */
@TestPropertySource(properties = "app.main.build=PROD")
public class UserRegistrationProdApiTest extends BaseUserTest {
  @BeforeEach
  public void tearDown() {
    resetDatabase();
  }

  // //////////////////////////////////////////////////////////////////////////

  @Test
  public void registerNewUserWithActivation() throws Exception {
    // Registering new user WITH simultaneous activation. Prod will simply ignore activation and will register user
    // normally.
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

    // Assert: API Response. Note it will pretend everything went fine.
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
}
