package org.portfolio.userland.features.user;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.portfolio.userland.common.services.jwt.JwtService;
import org.portfolio.userland.features.user.entities.EnHistoryWhat;
import org.portfolio.userland.features.user.entities.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Integration test for user logout.
 */
public class UserLogoutApiTest extends BaseUserTest {
  @Autowired
  private JwtService jwtService;

  @AfterEach
  public void tearDown() {
    cleanDatabase();
  }

  // //////////////////////////////////////////////////////////////////////////

  @Test
  public void logoutUser() throws Exception {
    clock.setFixedTime("2026-04-10T10:00:00Z");
    // Arrange: Create active user in database that is already logged in.
    User expectedUser = userFactory.genUser();

    clock.setFixedTime("2026-04-10T11:00:00Z");
    // Arrange: Manually login that user.
    String token = jwtService.generateToken(expectedUser);
    userHistoryFactory.genHistoryEvent(expectedUser, EnHistoryWhat.LOGIN);
    userJwtFactory.genJwtEntry(expectedUser, token);
    userRepository.save(expectedUser);

    clock.setFixedTime("2026-04-10T11:00:00Z");
    // Act: Log out user.
    MvcResult mvcResult = mockMvc.perform(post("/api/users/logout")
            .header("Authorization", "Bearer " + token))
        .andReturn();

    // Assert: API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.OK.value());

    // Prepare expected result (user is same, but with new LOGOUT history event and with empty JWT table).
    userHistoryFactory.genHistoryEvent(expectedUser, EnHistoryWhat.LOGOUT);
    expectedUser.getJwt().clear();

    // Assert: Database state.
    transactionTemplate.execute(_ -> {
      // Assert: User state.
      User actualUser = userRepository.findByEmail("test@example.com").orElseThrow();
      userAssert.assertIt(actualUser, expectedUser);
      return null;
    });
  }

  @Test
  public void logoutEmpty() throws Exception {
    clock.setFixedTime("2026-04-10T10:00:00Z");

    // Act: Call log out endpoint by itself. You are not logged in or anything.
    MvcResult mvcResult = mockMvc.perform(post("/api/users/logout"))
        .andReturn();

    // Assert: API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.OK.value());
  }
}
