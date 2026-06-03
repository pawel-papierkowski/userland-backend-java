package org.portfolio.userland.features.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.portfolio.userland.features.user.dto.common.UserDataResp;
import org.portfolio.userland.features.user.dto.common.UserProfileData;
import org.portfolio.userland.features.user.dto.standard.edit.UserEditReq;
import org.portfolio.userland.features.user.entities.*;
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
  @BeforeEach
  public void tearDown() {
    resetDatabase();
  }

  // //////////////////////////////////////////////////////////////////////////

  @Test
  @WithMockCustomUser
  public void editUserNoTrueChanges() throws Exception {
    // Change fields, but they all have same value.
    clock.setFixedTime("2026-04-10T10:00:00Z");

    // Arrange: Create active user and profile.
    User expectedUser = userFactory.genUser(EnUserStatus.ACTIVE);
    UserProfile expectedUserProfile = userProfileFactory.genRandProfile(expectedUser);
    userProfileRepository.save(expectedUserProfile);

    clock.setFixedTime("2026-04-10T10:05:00Z");

    // Arrange: Create request for editing of user account.
    UserProfileData profileReq = UserProfileData.builder().build();
    UserEditReq req = UserEditReq.builder()
        .username("Jane") // same username
        .lang("en") // same lang
        .profile(profileReq)
        .build();

    // Act: Try to edit user account.
    MvcResult mvcResult = mockMvc.perform(patch("/api/users/edit")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andReturn();

    // Assert: API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.OK.value());

    // Assert: Verify that endpoint response is correct.
    UserDataResp actualResp = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), UserDataResp.class);
    UserProfileData expectedProfile = userMapper.profileToData(expectedUserProfile); // random profile used
    UserDataResp expectedResp = UserDataResp.builder().username("Jane").email("test@example.com").lang("en").profile(expectedProfile).build();
    assertThat(actualResp).as("User data is invalid").usingRecursiveComparison().isEqualTo(expectedResp);

    // Assert: Database state.
    transactionTemplate.execute(_ -> {
      // Assert: User state.
      User user = assertAllUser("test@example.com", expectedUser, expectedUserProfile);
      assertThat(user.getPassword()).as("Password hash should be same").isEqualTo(expectedUser.getPassword());
      return null;
    });
  }

  @Test
  @WithMockCustomUser
  public void editUserAll() throws Exception {
    // Fully edit user, changing all available fields.
    clock.setFixedTime("2026-04-10T10:00:00Z");

    // Arrange: Create active user and profile.
    User expectedUser = userFactory.genUser(EnUserStatus.ACTIVE);
    UserProfile expectedUserProfile = userProfileFactory.genRandProfile(expectedUser);
    userProfileRepository.save(expectedUserProfile);

    clock.setFixedTime("2026-04-10T10:05:00Z");

    // Arrange: Create request for editing of user account.
    UserProfileData profileReq = UserProfileData.builder()
        .name("Jasiu")
        .surname("Fasola")
        .build();
    UserEditReq req = UserEditReq.builder()
        .username("JasiuFasola44")
        .lang("pl")
        .profile(profileReq)
        .build();

    // Act: Try to edit user account.
    MvcResult mvcResult = mockMvc.perform(patch("/api/users/edit")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andReturn();

    // Assert: API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.OK.value());

    // Assert: Verify that endpoint response is correct.
    UserDataResp actualResp = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), UserDataResp.class);
    UserProfileData expectedProfile = UserProfileData.builder().name("Jasiu").surname("Fasola").build();
    UserDataResp expectedResp = UserDataResp.builder().username("JasiuFasola44").email("test@example.com").lang("pl").profile(expectedProfile).build();
    assertThat(actualResp).as("User data is invalid").usingRecursiveComparison().isEqualTo(expectedResp);

    // Prepare expected result.
    expectedUser.setModifiedAt(clockService.getNowUTC());
    expectedUser.setUsername("JasiuFasola44");
    expectedUser.setLang("pl");
    expectedUserProfile.setName("Jasiu");
    expectedUserProfile.setSurname("Fasola");
    userHistoryFactory.genHistoryEvent(expectedUser, EnUserHistoryWho.USER, EnUserHistoryWhat.EDIT, "username, lang, name, surname");

    // Assert: Database state.
    transactionTemplate.execute(_ -> {
      // Assert: User state.
      User user = assertAllUser("test@example.com", expectedUser, expectedUserProfile);
      assertThat(user.getPassword()).as("Password hash should be same").isEqualTo(expectedUser.getPassword());
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
    UserProfileData profileReq = UserProfileData.builder().build();
    UserEditReq req = UserEditReq.builder()
        .username("Robert")
        .profile(profileReq)
        .build();

    // Act: Try to edit user account.
    MvcResult mvcResult = mockMvc.perform(patch("/api/users/edit")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andReturn();

    // Assert: API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.OK.value());

    // Assert: Verify that endpoint response is correct.
    UserDataResp actualResp = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), UserDataResp.class);
    UserProfileData expectedProfile = userMapper.profileToData(expectedUserProfile); // random profile used
    UserDataResp expectedResp = UserDataResp.builder().username("Robert").email("test@example.com").lang("en").profile(expectedProfile).build();
    assertThat(actualResp).as("User data is invalid").usingRecursiveComparison().isEqualTo(expectedResp);

    // Prepare expected result.
    expectedUser.setModifiedAt(clockService.getNowUTC());
    expectedUser.setUsername("Robert");
    userHistoryFactory.genHistoryEvent(expectedUser, EnUserHistoryWho.USER, EnUserHistoryWhat.EDIT, "username");

    // Assert: Database state.
    transactionTemplate.execute(_ -> {
      // Assert: User state.
      User user = assertAllUser("test@example.com", expectedUser, expectedUserProfile);
      assertThat(user.getPassword()).as("Password hash should be same").isEqualTo(expectedUser.getPassword());
      return null;
    });
  }

  @Test
  @WithMockCustomUser
  public void editUserSameValue() throws Exception {
    // Edit user: set two fields (username, lang), one of them to same value as previously (username).
    clock.setFixedTime("2026-04-10T10:00:00Z");

    // Arrange: Create active user and profile.
    User expectedUser = userFactory.genUser(EnUserStatus.ACTIVE);
    UserProfile expectedUserProfile = userProfileFactory.genRandProfile(expectedUser);
    userProfileRepository.save(expectedUserProfile);

    clock.setFixedTime("2026-04-10T10:05:00Z");

    // Arrange: Create request for editing of user account.
    UserEditReq req = UserEditReq.builder()
        .username("Jane")
        .lang("pl")
        .profile(null)
        .build();

    // Act: Try to edit user account.
    MvcResult mvcResult = mockMvc.perform(patch("/api/users/edit")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andReturn();

    // Assert: API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.OK.value());

    // Assert: Verify that endpoint response is correct.
    UserDataResp actualResp = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), UserDataResp.class);
    UserProfileData expectedProfile = userMapper.profileToData(expectedUserProfile); // random profile used
    UserDataResp expectedResp = UserDataResp.builder().username("Jane").email("test@example.com").lang("pl").profile(expectedProfile).build();
    assertThat(actualResp).as("User data is invalid").usingRecursiveComparison().isEqualTo(expectedResp);

    // Prepare expected result.
    expectedUser.setModifiedAt(clockService.getNowUTC());
    expectedUser.setLang("pl");
    // note there is only one field (username is same)
    userHistoryFactory.genHistoryEvent(expectedUser, EnUserHistoryWho.USER, EnUserHistoryWhat.EDIT, "lang");

    // Assert: Database state.
    transactionTemplate.execute(_ -> {
      // Assert: User state.
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
    UserProfileData profileReq = UserProfileData.builder().name("Tom").build();
    UserEditReq req = UserEditReq.builder().profile(profileReq).build();

    // Act: Try to edit user account.
    MvcResult mvcResult = mockMvc.perform(patch("/api/users/edit")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andReturn();

    // Assert: API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.OK.value());

    // Assert: Verify that endpoint response is correct.
    UserDataResp actualResp = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), UserDataResp.class);
    UserProfileData expectedProfile = userMapper.profileToData(expectedUserProfile); // random profile used
    expectedProfile = expectedProfile.toBuilder().name("Tom").build(); // only one field changed
    UserDataResp expectedResp = UserDataResp.builder().username("Jane").email("test@example.com").lang("en").profile(expectedProfile).build();
    assertThat(actualResp).as("User data is invalid").usingRecursiveComparison().isEqualTo(expectedResp);

    // Prepare expected result.
    expectedUser.setModifiedAt(clockService.getNowUTC());
    expectedUserProfile.setName("Tom");
    userHistoryFactory.genHistoryEvent(expectedUser, EnUserHistoryWho.USER, EnUserHistoryWhat.EDIT, "name");

    // Assert: Database state.
    transactionTemplate.execute(_ -> {
      // Assert: User state.
      User user = assertAllUser("test@example.com", expectedUser, expectedUserProfile);
      assertThat(user.getPassword()).as("Password hash should be same").isEqualTo(expectedUser.getPassword());
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
    UserEditReq req = UserEditReq.builder().build();

    // Act: Try to edit user account.
    MvcResult mvcResult = mockMvc.perform(patch("/api/users/edit")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andReturn();

    // Assert: API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.OK.value());

    // Assert: Verify that endpoint response is correct.
    UserDataResp actualResp = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), UserDataResp.class);
    UserProfileData expectedProfile = userMapper.profileToData(expectedUserProfile); // random profile used
    UserDataResp expectedResp = UserDataResp.builder().username("Jane").email("test@example.com").lang("en").profile(expectedProfile).build();
    assertThat(actualResp).as("User data is invalid").usingRecursiveComparison().isEqualTo(expectedResp);

    // Assert: Database state.
    transactionTemplate.execute(_ -> {
      // Assert: User state.
      User user = assertAllUser("test@example.com", expectedUser, expectedUserProfile);
      assertThat(user.getPassword()).as("Password hash should be same").isEqualTo(expectedUser.getPassword());
      return null;
    });
  }
}
