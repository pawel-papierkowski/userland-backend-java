package org.portfolio.userland.test.features.user;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.portfolio.userland.base.BaseIntegrationTest;
import org.portfolio.userland.features.user.data.EnUserStatus;
import org.portfolio.userland.features.user.data.User;
import org.portfolio.userland.features.user.dto.UserRegisterReq;
import org.portfolio.userland.features.user.dto.UserRegisterResp;
import org.portfolio.userland.features.user.repositories.UserRepository;
import org.portfolio.userland.utils.problemDetail.ProblemDetailBox;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Integration test for user registration.
 */
public class UserRegistrationApiTest extends BaseIntegrationTest {
  @Autowired
  private UserRepository userRepository;

  @AfterEach
  void tearDown() {
    // Clean up the database after every test so tests don't interfere with each other.
    userRepository.deleteAll();
  }

  //

  @Test
  void registerNewUser() throws Exception {
    clock.setFixedTime("2026-04-10T10:00:00Z");

    // Arrange: Create the JSON payload.
    UserRegisterReq req = new UserRegisterReq("testuser", "testuser@example.com", "SecurePass123!");

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

    // Assert Database State.
    boolean userExists = userRepository.existsByEmail("testuser@example.com");
    assertThat(userExists).isTrue();

    User actualUser = userRepository.findByEmail("testuser@example.com").orElseThrow();
    assertThat(actualUser.getId()).as("Id is wrong").isEqualTo(actualResp.id());
    assertThat(actualUser.getCreatedAt().toString()).as("CreatedAt is wrong").isEqualTo("2026-04-10T10:00");
    assertThat(actualUser.getModifiedAt().toString()).as("ModifiedAt is wrong").isEqualTo("2026-04-10T10:00");
    assertThat(actualUser.getUsername()).as("Username is wrong").isEqualTo("testuser");
    assertThat(actualUser.getEmail()).as("Email is wrong").isEqualTo("testuser@example.com");
    assertThat(actualUser.getPassword()).isNotEqualTo("SecurePass123!");
    assertThat(actualUser.getPassword()).startsWith("$2a$"); // Ensure MapStruct + BCrypt hashed the password!
    assertThat(actualUser.getStatus()).as("Status is wrong").isEqualTo(EnUserStatus.PENDING);
    assertThat(actualUser.getBlocked()).as("Blocked is wrong").isFalse();
  }

  // FAILURES

  @Test
  void errUserWithEmailAlreadyExists() throws Exception {
    clock.setFixedTime("2026-04-10T10:00:00Z");

    // Arrange: Create existing user manually.
    User existingUser = new User();
    existingUser.setCreatedAt(clockService.getNowUTC());
    existingUser.setModifiedAt(clockService.getNowUTC());
    existingUser.setUsername("Jane");
    existingUser.setEmail("duplicate@example.com");
    existingUser.setPassword("hashedPassword");
    userRepository.save(existingUser);

    // Arrange: Create the JSON payload.
    UserRegisterReq req = new UserRegisterReq("testuser", "duplicate@example.com", "SecurePass123!");

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
        "Email 'duplicate@example.com' already exists.",
        "/api/users/register",
        "https://api.userland.org/errors/general",
        Map.of()
    );
    problemDetailService.assertPd(mvcResult, expectedPdb);
  }
}
