package org.portfolio.userland.features.user.admin;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.portfolio.userland.features.user.BaseUserTest;
import org.portfolio.userland.features.user.dto.admin.user.UserFullDataResp;
import org.portfolio.userland.features.user.entities.EnUserStatus;
import org.portfolio.userland.features.user.entities.User;
import org.portfolio.userland.features.user.entities.UserProfile;
import org.portfolio.userland.test.helpers.context.WithMockCustomUser;
import org.portfolio.userland.test.helpers.problemDetail.ProblemDetailBox;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * Integration test for retrieving almost all data for any user.
 */
public class UserFullDataApiTest extends BaseUserTest {
  @BeforeEach
  public void tearDown() {
    resetDatabase();
  }

  // //////////////////////////////////////////////////////////////////////////

  @Test
  @WithMockCustomUser(authorities = { "ROLE_OPERATOR" })
  public void viewUserData() throws Exception {
    // Arrange: Create user and user profile.
    clock.setFixedTime("2026-04-10T10:00:00Z");
    User expectedUser = userFactory.genUser(EnUserStatus.ACTIVE);
    UserProfile expectedUserProfile = userProfileFactory.genProfile(expectedUser);

    UserProfile userProfile = userProfileRepository.save(expectedUserProfile);
    User user = userProfile.getUser();

    // Act: Get user data.
    MvcResult mvcResult = mockMvc.perform(get("/api/admin/users/"+user.getId()))
        .andReturn();

    // Assert: API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.OK.value());
    // Assert: Verify that result is correct.
    UserFullDataResp actualResp = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), UserFullDataResp.class);
    UserFullDataResp expectedResp = userAdminFactory.genFullData(user, userProfile);
    assertThat(actualResp).as("User full data is invalid").usingRecursiveComparison().isEqualTo(expectedResp);
  }

  // //////////////////////////////////////////////////////////////////////////
  // FAILURES

  @Test
  @WithMockCustomUser(authorities = { "ROLE_OPERATOR" })
  public void wrongId() throws Exception {
    // Arrange: None needed.

    // Act: Try to get non-existent user data.
    MvcResult mvcResult = mockMvc.perform(get("/api/admin/users/1"))
        .andReturn();

    // Assert: API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.NOT_FOUND.value());
    // Assert: Content has correct error.
    ProblemDetailBox expectedPdb = new ProblemDetailBox(
        HttpStatus.NOT_FOUND.value(),
        "User cannot be found.",
        "User with id '1' does not exist.",
        "/api/admin/users/1",
        "https://api.userland.org/errors/user/doesNotExist",
        Map.of("errCode", "user_0001")
    );
    problemDetailService.assertPd(mvcResult, expectedPdb);
  }
}
