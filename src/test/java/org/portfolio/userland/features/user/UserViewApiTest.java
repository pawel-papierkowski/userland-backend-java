package org.portfolio.userland.features.user;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.portfolio.userland.features.user.dto.common.UserDataResp;
import org.portfolio.userland.features.user.dto.common.UserProfileDataResp;
import org.portfolio.userland.features.user.entities.EnUserStatus;
import org.portfolio.userland.features.user.entities.User;
import org.portfolio.userland.features.user.entities.UserProfile;
import org.portfolio.userland.test.helpers.context.WithMockCustomUser;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Integration test for viewing user account.
 */
public class UserViewApiTest extends BaseUserTest {
  @AfterEach
  public void tearDown() {
    resetDatabase();
  }

  // //////////////////////////////////////////////////////////////////////////

  @Test
  @WithMockCustomUser
  public void viewUser() throws Exception {
    // View user with minimal data, including empty profile.
    clock.setFixedTime("2026-04-10T10:00:00Z");

    // Arrange: Create active user and profile.
    User expectedUser = userFactory.genUser(EnUserStatus.ACTIVE);
    UserProfile expectedUserProfile = userProfileFactory.genProfile(expectedUser);
    userProfileRepository.save(expectedUserProfile);

    // Act: Try to view user account.
    MvcResult mvcResult = mockMvc.perform(get("/api/users/view"))
        .andReturn();

    // Assert API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.OK.value());

    // Assert: Endpoint response.
    UserDataResp actualResp = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), UserDataResp.class);
    UserProfileDataResp profile = UserProfileDataResp.builder().build(); // empty profile
    UserDataResp expectedResp = new UserDataResp("Jane", "test@example.com", "en", profile);
    assertThat(actualResp).as("User data is invalid").usingRecursiveComparison().isEqualTo(expectedResp);
  }

  @Test
  @WithMockCustomUser
  public void viewFullUser() throws Exception {
    // View user with full profile.
    clock.setFixedTime("2026-04-10T10:00:00Z");

    // Arrange: Create active user and profile.
    User expectedUser = userFactory.genUser(EnUserStatus.ACTIVE);
    UserProfile expectedUserProfile = userProfileFactory.genProfile(expectedUser, "Jennifer", "Doe");
    userProfileRepository.save(expectedUserProfile);

    // Act: Try to view user account.
    MvcResult mvcResult = mockMvc.perform(get("/api/users/view"))
        .andReturn();

    // Assert API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.OK.value());

    // Assert: Endpoint response.
    UserDataResp actualResp = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), UserDataResp.class);
    UserProfileDataResp profile = new UserProfileDataResp("Jennifer", "Doe");
    UserDataResp expectedResp = new UserDataResp("Jane", "test@example.com", "en", profile);
    assertThat(actualResp).as("User data is invalid").usingRecursiveComparison().isEqualTo(expectedResp);
  }
}
