package org.portfolio.userland.features.user;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.portfolio.userland.features.user.dto.common.UserDataResp;
import org.portfolio.userland.features.user.dto.common.UserProfileDataResp;
import org.portfolio.userland.features.user.dto.edit.UserEditReq;
import org.portfolio.userland.features.user.entities.EnUserHistoryWhat;
import org.portfolio.userland.features.user.entities.EnUserStatus;
import org.portfolio.userland.features.user.entities.User;
import org.portfolio.userland.features.user.entities.UserProfile;
import org.portfolio.userland.test.helpers.context.WithMockCustomUser;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;

/**
 * Integration test for editing user account.
 */
public class UserEditApiTest extends BaseUserTest {
  @AfterEach
  public void tearDown() {
    resetDatabase();
  }

  // //////////////////////////////////////////////////////////////////////////

  @Test
  @WithMockCustomUser
  public void fullEditUser() throws Exception {
    // Fully edit user, changing all available fields.
    clock.setFixedTime("2026-04-10T10:00:00Z");

    // Arrange: Create active user and profile.
    User expectedUser = userFactory.genUser(EnUserStatus.ACTIVE);
    UserProfile expectedUserProfile = userProfileFactory.genRandProfile(expectedUser);
    userProfileRepository.save(expectedUserProfile);

    clock.setFixedTime("2026-04-10T10:05:00Z");

    // Arrange: Create request for editing of user account.
    UserEditReq req = new UserEditReq("JasiuFasola44", "#eWp@s5w0rD", "pl", "Jasiu", "Fasola");

    // Act: Try to edit user account.
    MvcResult mvcResult = mockMvc.perform(patch("/api/users/edit")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andReturn();

    // Assert API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.OK.value());

    // Assert: Endpoint response.
    UserDataResp actualResp = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), UserDataResp.class);
    UserProfileDataResp profile = new UserProfileDataResp("Jasiu", "Fasola");
    UserDataResp expectedResp = new UserDataResp("JasiuFasola44", "test@example.com", "pl", profile);
    assertThat(actualResp).as("User data is invalid").usingRecursiveComparison().isEqualTo(expectedResp);

    // Prepare expected result.
    expectedUser.setModifiedAt(clockService.getNowUTC());
    expectedUser.setUsername("JasiuFasola44");
    expectedUser.setLang("pl");
    expectedUserProfile.setName("Jasiu");
    expectedUserProfile.setSurname("Fasola");
    userHistoryFactory.genHistoryEvent(expectedUser, EnUserHistoryWhat.EDIT, "username, password, lang, name, surname");

    // Assert that user data is correctly updated.
    transactionTemplate.execute(_ -> {
      // Assert user state.
      User user = assertAllUser("test@example.com", expectedUser, expectedUserProfile);
      assertThat(user.getPassword()).as("Password hash should be different").isNotEqualTo(expectedUser.getPassword());
      return null;
    });
  }

  @Test
  @WithMockCustomUser
  public void editUserOneField() throws Exception {
    // Edit user: change only single field (username).
    clock.setFixedTime("2026-04-10T10:00:00Z");

    // Arrange: Create active user and profile.
    User expectedUser = userFactory.genUser(EnUserStatus.ACTIVE);
    UserProfile expectedUserProfile = userProfileFactory.genRandProfile(expectedUser);
    userProfileRepository.save(expectedUserProfile);

    clock.setFixedTime("2026-04-10T10:05:00Z");

    // Arrange: Create request for editing of user account.
    UserEditReq req = new UserEditReq("Robert", null, null, null, null);

    // Act: Try to edit user account.
    MvcResult mvcResult = mockMvc.perform(patch("/api/users/edit")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andReturn();

    // Assert API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.OK.value());

    // Assert: Endpoint response.
    UserDataResp actualResp = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), UserDataResp.class);
    UserDataResp expectedResp = new UserDataResp("Robert", "test@example.com", "en", null);
    assertThat(actualResp).as("User data is invalid").usingRecursiveComparison().isEqualTo(expectedResp);

    // Prepare expected result.
    expectedUser.setModifiedAt(clockService.getNowUTC());
    expectedUser.setUsername("Robert");
    userHistoryFactory.genHistoryEvent(expectedUser, EnUserHistoryWhat.EDIT, "username");

    // Assert that user data is correctly updated.
    transactionTemplate.execute(_ -> {
      // Assert user state.
      User user = assertAllUser("test@example.com", expectedUser, expectedUserProfile);
      assertThat(user.getPassword()).as("Password hash should be same").isEqualTo(expectedUser.getPassword());
      return null;
    });
  }

  @Test
  @WithMockCustomUser
  public void editUserProfileOneField() throws Exception {
    // Edit user: change only single field, but in profile.
    clock.setFixedTime("2026-04-10T10:00:00Z");

    // Arrange: Create active user and profile.
    User expectedUser = userFactory.genUser(EnUserStatus.ACTIVE);
    UserProfile expectedUserProfile = userProfileFactory.genRandProfile(expectedUser);
    userProfileRepository.save(expectedUserProfile);

    clock.setFixedTime("2026-04-10T10:05:00Z");

    // Arrange: Create request for editing of user account.
    UserEditReq req = new UserEditReq(null, null, null, "Tom", null);

    // Act: Try to edit user account.
    MvcResult mvcResult = mockMvc.perform(patch("/api/users/edit")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andReturn();

    // Assert API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.OK.value());

    // Assert: Endpoint response.
    UserDataResp actualResp = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), UserDataResp.class);
    UserProfileDataResp profile = userProfileMapper.userToDataResp(expectedUserProfile);
    profile = profile.toBuilder().name("Tom").build(); // only one field changed
    UserDataResp expectedResp = new UserDataResp("Jane", "test@example.com", "en", profile);
    assertThat(actualResp).as("User data is invalid").usingRecursiveComparison().isEqualTo(expectedResp);

    // Prepare expected result.
    expectedUser.setModifiedAt(clockService.getNowUTC());
    expectedUserProfile.setName("Tom");
    userHistoryFactory.genHistoryEvent(expectedUser, EnUserHistoryWhat.EDIT, "name");

    // Assert that user data is correctly updated.
    transactionTemplate.execute(_ -> {
      // Assert user state.
      User user = assertAllUser("test@example.com", expectedUser, expectedUserProfile);
      assertThat(user.getPassword()).as("Password hash should be same").isEqualTo(expectedUser.getPassword());
      return null;
    });
  }

  @Test
  @WithMockCustomUser
  public void editUserPasswordOnly() throws Exception {
    // Edit user: change only password.
    clock.setFixedTime("2026-04-10T10:00:00Z");

    // Arrange: Create active user and profile.
    User expectedUser = userFactory.genUser(EnUserStatus.ACTIVE);
    UserProfile expectedUserProfile = userProfileFactory.genRandProfile(expectedUser);
    userProfileRepository.save(expectedUserProfile);

    clock.setFixedTime("2026-04-10T10:05:00Z");

    // Arrange: Create request for editing of user account.
    UserEditReq req = new UserEditReq(null, "#eWp@s5w0rD", null, null, null);

    // Act: Try to edit user account.
    MvcResult mvcResult = mockMvc.perform(patch("/api/users/edit")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andReturn();

    // Assert API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.OK.value());

    // Assert: Endpoint response.
    UserDataResp actualResp = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), UserDataResp.class);
    UserDataResp expectedResp = new UserDataResp("Jane", "test@example.com", "en", null); // unchanged
    assertThat(actualResp).as("User data is invalid").usingRecursiveComparison().isEqualTo(expectedResp);

    // Prepare expected result.
    expectedUser.setModifiedAt(clockService.getNowUTC());
    userHistoryFactory.genHistoryEvent(expectedUser, EnUserHistoryWhat.EDIT, "password");

    // Assert that user data is correctly updated.
    transactionTemplate.execute(_ -> {
      // Assert user state.
      User user = assertAllUser("test@example.com", expectedUser, expectedUserProfile);
      assertThat(user.getPassword()).as("Password hash should be different").isNotEqualTo(expectedUser.getPassword());
      return null;
    });
  }

  @Test
  @WithMockCustomUser
  public void editUserNoFields() throws Exception {
    // Edit user: no fields changed.
    clock.setFixedTime("2026-04-10T10:00:00Z");

    // Arrange: Create active user and profile.
    User expectedUser = userFactory.genUser(EnUserStatus.ACTIVE);
    UserProfile expectedUserProfile = userProfileFactory.genRandProfile(expectedUser);
    userProfileRepository.save(expectedUserProfile);

    clock.setFixedTime("2026-04-10T10:05:00Z");

    // Arrange: Create request for editing of user account.
    UserEditReq req = new UserEditReq(null, null, null, null, null);

    // Act: Try to edit user account.
    MvcResult mvcResult = mockMvc.perform(patch("/api/users/edit")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andReturn();

    // Assert API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.OK.value());

    // Assert: Endpoint response.
    UserDataResp actualResp = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), UserDataResp.class);
    UserDataResp expectedResp = new UserDataResp("Jane", "test@example.com", "en", null); // unchanged
    assertThat(actualResp).as("User data is invalid").usingRecursiveComparison().isEqualTo(expectedResp);

    // Assert that user data is correctly updated.
    transactionTemplate.execute(_ -> {
      // Assert user state.
      User user = assertAllUser("test@example.com", expectedUser, expectedUserProfile);
      assertThat(user.getPassword()).as("Password hash should be same").isEqualTo(expectedUser.getPassword());
      return null;
    });
  }
}
