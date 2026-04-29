package org.portfolio.userland.features.user;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.portfolio.userland.features.user.entities.EnUserHistoryWhat;
import org.portfolio.userland.features.user.entities.User;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Integration test for user logout.
 */
public class UserLogoutApiTest extends BaseUserTest {
  @AfterEach
  public void tearDown() {
    resetDatabase();
  }

  // //////////////////////////////////////////////////////////////////////////

  @Test
  public void logoutUser() throws Exception {
    clock.setFixedTime("2026-04-10T10:00:00Z");
    // Arrange: Create active user in database that is already logged in.
    User expectedUser = userFactory.genUserLogged();
    String token = expectedUser.getJwts().stream().toList().getFirst().getToken();
    userRepository.save(expectedUser);

    clock.setFixedTime("2026-04-10T11:00:00Z");
    // Act: Log out user.
    MvcResult mvcResult = mockMvc.perform(post("/api/users/logout")
            .header("Authorization", "Bearer " + token))
        .andReturn();

    // Assert: API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.NO_CONTENT.value());

    // Prepare expected result (user is same, but with new LOGOUT history event and with empty JWT table).
    userHistoryFactory.genHistoryEvent(expectedUser, EnUserHistoryWhat.LOGOUT, "");
    expectedUser.getJwts().clear();

    // Assert: Database state.
    transactionTemplate.execute(_ -> {
      // Assert: User state.
      User actualUser = userRepository.findByEmail(expectedUser.getEmail()).orElseThrow();
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
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.NO_CONTENT.value());
  }
}
