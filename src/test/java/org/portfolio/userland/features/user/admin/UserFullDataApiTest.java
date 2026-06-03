package org.portfolio.userland.features.user.admin;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.portfolio.userland.features.user.BaseUserTest;
import org.portfolio.userland.features.user.dto.admin.user.UserFullDataReq;
import org.portfolio.userland.features.user.dto.admin.user.UserFullDataResp;
import org.portfolio.userland.features.user.dto.common.UserProfileData;
import org.portfolio.userland.features.user.entities.*;
import org.portfolio.userland.test.helpers.context.WithMockCustomUser;
import org.portfolio.userland.test.helpers.problemDetail.ProblemDetailBox;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;

/**
 * Integration test for handling almost all data for any user.
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
    // We want to load almost all data of given user.
    // Arrange: Create user and user profile.
    clock.setFixedTime("2026-04-10T10:00:00Z");
    User expectedUser = userFactory.genUser(EnUserStatus.ACTIVE);
    UserProfile expectedUserProfile = userProfileFactory.genProfile(expectedUser);

    UserProfile userProfile = userProfileRepository.save(expectedUserProfile);
    User user = userProfile.getUser();

    // Act: Get user data.
    MvcResult mvcResult = mockMvc.perform(get("/api/admin/user/"+user.getId()))
        .andReturn();

    // Assert: API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.OK.value());

    // Assert: Verify that endpoint response is correct.
    UserFullDataResp actualResp = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), UserFullDataResp.class);
    UserFullDataResp expectedResp = userAdminFactory.genFullData(user, userProfile);
    assertThat(actualResp).as("User full data is invalid").usingRecursiveComparison().isEqualTo(expectedResp);
  }

  //

  @Test
  @WithMockCustomUser(authorities = { "ROLE_OPERATOR" })
  public void editUserNoTrueChanges() throws Exception {
    // We do not change anything, actually. Request has same data as existing user.
    // Arrange: Create user and user profile.
    clock.setFixedTime("2026-04-10T10:00:00Z");
    User expectedUser = userFactory.genUser(EnUserStatus.ACTIVE);
    UserProfile expectedUserProfile = userProfileFactory.genProfile(expectedUser);

    UserProfile userProfile = userProfileRepository.save(expectedUserProfile);
    User user = userProfile.getUser();

    // Arrange: Create request to change user and user profile data.
    UserFullDataReq req = UserFullDataReq.builder()
        .id(user.getId())
        .username("Jane") // same username
        .email("test@example.com") // same email as previous for this account, code checks for that
        .build();

    // Act: Try to change data of existing user.
    clock.setFixedTime("2026-04-12T10:00:00Z");
    MvcResult mvcResult = mockMvc.perform(patch("/api/admin/user")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andReturn();

    // Assert: API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.OK.value());

    // Assert: Verify that endpoint response is correct.
    UserFullDataResp actualResp = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), UserFullDataResp.class);
    UserFullDataResp expectedResp = userAdminFactory.genFullData(expectedUser, expectedUserProfile);
    assertThat(actualResp).as("User full data is invalid").usingRecursiveComparison().isEqualTo(expectedResp);

    // Assert: Database state.
    transactionTemplate.execute(_ -> {
      // Assert: User state.
      assertAllUser(user.getEmail(), expectedUser, expectedUserProfile);
      return null;
    });
  }

  @Test
  @WithMockCustomUser(authorities = { "ROLE_OPERATOR" })
  public void editUserAll() throws Exception {
    // We want to change almost all data of given user.
    // Arrange: Create user and user profile.
    clock.setFixedTime("2026-04-10T10:00:00Z");
    User expectedUser = userFactory.genUser(EnUserStatus.ACTIVE);
    UserProfile expectedUserProfile = userProfileFactory.genProfile(expectedUser);

    UserProfile userProfile = userProfileRepository.save(expectedUserProfile);
    User user = userProfile.getUser();

    // Arrange: Create request to change user and user profile data.
    UserProfileData profileData = UserProfileData.builder()
        .name("Robert")
        .surname("Novak")
        .build();
    UserFullDataReq req = UserFullDataReq.builder()
        .id(user.getId())
        .username("Monke")
        .email("different.email@example.com")
        .locked(true)
        .lang("fr")
        .profile(profileData)
        .build();

    // Act: Try to change data of existing user.
    clock.setFixedTime("2026-04-12T10:00:00Z");
    MvcResult mvcResult = mockMvc.perform(patch("/api/admin/user")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andReturn();

    // Assert: API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.OK.value());

    // Prepare expected entities manually.
    expectedUser.setModifiedAt(clockService.getNowUTC());
    expectedUser.setUsername("Monke");
    expectedUser.setEmail("different.email@example.com");
    expectedUser.setLocked(true);
    expectedUser.setLang("fr");
    expectedUserProfile.setName("Robert");
    expectedUserProfile.setSurname("Novak");
    userHistoryFactory.genHistoryEvent(expectedUser, EnUserHistoryWho.OPERATOR, EnUserHistoryWhat.EDIT, "username, email, locked, lang, name, surname");

    // Assert: Verify that endpoint response is correct.
    UserFullDataResp actualResp = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), UserFullDataResp.class);
    UserFullDataResp expectedResp = userAdminFactory.genFullData(expectedUser, expectedUserProfile);
    assertThat(actualResp).as("User full data is invalid").usingRecursiveComparison().isEqualTo(expectedResp);

    // Assert: Database state.
    transactionTemplate.execute(_ -> {
      // Assert: User state.
      assertAllUser(user.getEmail(), expectedUser, expectedUserProfile);
      return null;
    });
  }

  @Test
  @WithMockCustomUser(authorities = { "ROLE_OPERATOR" })
  public void editUserSome() throws Exception {
    // We want to change some data of given user.
    // Arrange: Create user and user profile.
    clock.setFixedTime("2026-04-10T10:00:00Z");
    User expectedUser = userFactory.genUser(EnUserStatus.ACTIVE);
    UserProfile expectedUserProfile = userProfileFactory.genProfile(expectedUser);

    UserProfile userProfile = userProfileRepository.save(expectedUserProfile);
    User user = userProfile.getUser();

    // Arrange: Create request to change user and user profile data.
    UserFullDataReq req = UserFullDataReq.builder()
        .id(user.getId())
        .username("Monke")
        .locked(false)
        .build();

    // Act: Try to change data of existing user.
    clock.setFixedTime("2026-04-12T10:00:00Z");
    MvcResult mvcResult = mockMvc.perform(patch("/api/admin/user")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andReturn();

    // Assert: API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.OK.value());

    // Prepare expected entities manually.
    expectedUser.setModifiedAt(clockService.getNowUTC());
    expectedUser.setUsername("Monke");
    // note there is only one field in params (locked is same)
    userHistoryFactory.genHistoryEvent(expectedUser, EnUserHistoryWho.OPERATOR, EnUserHistoryWhat.EDIT, "username");

    // Assert: Verify that endpoint response is correct.
    UserFullDataResp actualResp = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), UserFullDataResp.class);
    UserFullDataResp expectedResp = userAdminFactory.genFullData(expectedUser, expectedUserProfile);
    assertThat(actualResp).as("User full data is invalid").usingRecursiveComparison().isEqualTo(expectedResp);

    // Assert: Database state.
    transactionTemplate.execute(_ -> {
      // Assert: User state.
      assertAllUser(user.getEmail(), expectedUser, expectedUserProfile);
      return null;
    });
  }

  @Test
  @WithMockCustomUser(authorities = { "ROLE_OPERATOR" })
  public void editUserProfile() throws Exception {
    // We want to edit only profile.
    // Arrange: Create user and user profile.
    clock.setFixedTime("2026-04-10T10:00:00Z");
    User expectedUser = userFactory.genUser(EnUserStatus.ACTIVE);
    UserProfile expectedUserProfile = userProfileFactory.genProfile(expectedUser);

    UserProfile userProfile = userProfileRepository.save(expectedUserProfile);
    User user = userProfile.getUser();

    // Arrange: Create request to change user and user profile data.
    UserProfileData profileData = UserProfileData.builder()
        .name("Robert")
        .surname("Novak")
        .build();
    UserFullDataReq req = UserFullDataReq.builder()
        .id(user.getId())
        .profile(profileData)
        .build();

    // Act: Try to change data of existing user.
    clock.setFixedTime("2026-04-12T10:00:00Z");
    MvcResult mvcResult = mockMvc.perform(patch("/api/admin/user")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andReturn();

    // Assert: API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.OK.value());

    // Prepare expected entities manually.
    expectedUser.setModifiedAt(clockService.getNowUTC());
    expectedUserProfile.setName("Robert");
    expectedUserProfile.setSurname("Novak");
    userHistoryFactory.genHistoryEvent(expectedUser, EnUserHistoryWho.OPERATOR, EnUserHistoryWhat.EDIT, "name, surname");

    // Assert: Verify that endpoint response is correct.
    UserFullDataResp actualResp = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), UserFullDataResp.class);
    UserFullDataResp expectedResp = userAdminFactory.genFullData(expectedUser, expectedUserProfile);
    assertThat(actualResp).as("User full data is invalid").usingRecursiveComparison().isEqualTo(expectedResp);

    // Assert: Database state.
    transactionTemplate.execute(_ -> {
      // Assert: User state.
      assertAllUser(user.getEmail(), expectedUser, expectedUserProfile);
      return null;
    });
  }

  @Test
  @WithMockCustomUser(authorities = { "ROLE_OPERATOR" })
  public void lockUser() throws Exception {
    // We want to lock user.
    // Arrange: Create user and user profile.
    clock.setFixedTime("2026-04-10T10:00:00Z");
    User expectedUser = userFactory.genUser(EnUserStatus.ACTIVE);
    UserProfile expectedUserProfile = userProfileFactory.genProfile(expectedUser);

    UserProfile userProfile = userProfileRepository.save(expectedUserProfile);
    User user = userProfile.getUser();

    // Arrange: Create request to lock user.
    UserFullDataReq req = UserFullDataReq.builder()
        .id(user.getId())
        .locked(true)
        .profile(null)
        .build();

    // Act: Try to change data of existing user.
    clock.setFixedTime("2026-04-12T10:00:00Z");
    MvcResult mvcResult = mockMvc.perform(patch("/api/admin/user")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andReturn();

    // Assert: API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.OK.value());

    // Prepare expected entities manually.
    expectedUser.setModifiedAt(clockService.getNowUTC());
    expectedUser.setLocked(true);
    userHistoryFactory.genHistoryEvent(expectedUser, EnUserHistoryWho.OPERATOR, EnUserHistoryWhat.EDIT, "locked");

    // Assert: Verify that endpoint response is correct.
    UserFullDataResp actualResp = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), UserFullDataResp.class);
    UserFullDataResp expectedResp = userAdminFactory.genFullData(expectedUser, expectedUserProfile);
    assertThat(actualResp).as("User full data is invalid").usingRecursiveComparison().isEqualTo(expectedResp);

    // Assert: Database state.
    transactionTemplate.execute(_ -> {
      // Assert: User state.
      assertAllUser(user.getEmail(), expectedUser, expectedUserProfile);
      return null;
    });
  }

  @Test
  @WithMockCustomUser(authorities = { "ROLE_OPERATOR" })
  public void editUserNoFields() throws Exception {
    // We do not change anything. Request has no fields.
    // Arrange: Create user and user profile.
    clock.setFixedTime("2026-04-10T10:00:00Z");
    User expectedUser = userFactory.genUser(EnUserStatus.ACTIVE);
    UserProfile expectedUserProfile = userProfileFactory.genProfile(expectedUser);

    UserProfile userProfile = userProfileRepository.save(expectedUserProfile);
    User user = userProfile.getUser();

    // Arrange: Create request to change... nothing.
    UserFullDataReq req = UserFullDataReq.builder().id(user.getId()).build();

    // Act: Try to change data of existing user.
    clock.setFixedTime("2026-04-12T10:00:00Z");
    MvcResult mvcResult = mockMvc.perform(patch("/api/admin/user")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andReturn();

    // Assert: API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.OK.value());

    // Assert: Verify that endpoint response is correct.
    UserFullDataResp actualResp = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), UserFullDataResp.class);
    UserFullDataResp expectedResp = userAdminFactory.genFullData(expectedUser, expectedUserProfile);
    assertThat(actualResp).as("User full data is invalid").usingRecursiveComparison().isEqualTo(expectedResp);

    // Assert: Database state.
    transactionTemplate.execute(_ -> {
      // Assert: User state.
      assertAllUser(user.getEmail(), expectedUser, expectedUserProfile);
      return null;
    });
  }

  // //////////////////////////////////////////////////////////////////////////
  // FAILURES

  @Test
  @WithMockCustomUser(authorities = { "ROLE_OPERATOR" })
  public void viewUserWithWrongId() throws Exception {
    // Arrange: None needed.

    // Act: Try to get non-existent user data.
    MvcResult mvcResult = mockMvc.perform(get("/api/admin/user/1"))
        .andReturn();

    // Assert: API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.NOT_FOUND.value());
    // Assert: Content has correct error.
    ProblemDetailBox expectedPdb = new ProblemDetailBox(
        HttpStatus.NOT_FOUND.value(),
        "User cannot be found.",
        "User with id '1' does not exist.",
        "/api/admin/user/1",
        "https://api.userland.org/errors/user/doesNotExist",
        Map.of("errCode", "user_0001")
    );
    problemDetailService.assertPd(mvcResult, expectedPdb);
  }

  @Test
  @WithMockCustomUser(authorities = { "ROLE_OPERATOR" })
  public void editUserWithWrongId() throws Exception {
    // Arrange: request that uses id of non-existent user.
    UserFullDataReq req = UserFullDataReq.builder().id(1L).build();

    // Act: Try to change data of non-existent user.
    MvcResult mvcResult = mockMvc.perform(patch("/api/admin/user")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andReturn();

    // Assert: API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.NOT_FOUND.value());
    // Assert: Content has correct error.
    ProblemDetailBox expectedPdb = new ProblemDetailBox(
        HttpStatus.NOT_FOUND.value(),
        "User cannot be found.",
        "User with id '1' does not exist.",
        "/api/admin/user",
        "https://api.userland.org/errors/user/doesNotExist",
        Map.of("errCode", "user_0001")
    );
    problemDetailService.assertPd(mvcResult, expectedPdb);
  }

  @Test
  @WithMockCustomUser(authorities = { "ROLE_OPERATOR" }) // note email of mock JWT is same as edited user
  public void editYourOwnUser() throws Exception {
    // We are trying to edit your own account, but that should not be allowed.
    // TODO: finish that test
    // Arrange: Create user and user profile.
    clock.setFixedTime("2026-04-10T10:00:00Z");
    User expectedUser = userFactory.genUser(EnUserStatus.ACTIVE);
    UserProfile expectedUserProfile = userProfileFactory.genProfile(expectedUser);

    UserProfile userProfile = userProfileRepository.save(expectedUserProfile);
    User user = userProfile.getUser();

    // Arrange: Create request to change user and user profile data.
    UserFullDataReq req = UserFullDataReq.builder()
        .id(user.getId())
        .username("Monke")
        .build();

    // Act: Try to change data of existing user.
    clock.setFixedTime("2026-04-12T10:00:00Z");
    MvcResult mvcResult = mockMvc.perform(patch("/api/admin/user")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andReturn();

    // Assert: API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.CONFLICT.value());
    // Assert: Content has correct error.
    ProblemDetailBox expectedPdb = new ProblemDetailBox(
        HttpStatus.CONFLICT.value(),
        "User with given email already exists.",
        "Email '' already exists.",
        "/api/admin/user",
        "https://api.userland.org/errors/user/email/alreadyExists",
        Map.of("errCode", "user_0111")
    );
    problemDetailService.assertPd(mvcResult, expectedPdb);
  }

  @Test
  @WithMockCustomUser(authorities = { "ROLE_OPERATOR" })
  public void editUserWithExistingEmail() throws Exception {
    // Arrange: Create first user and user profile.
    clock.setFixedTime("2026-04-10T10:00:00Z");
    User user1 = userFactory.genRandUser(EnUserStatus.ACTIVE);
    UserProfile userProfile1 = userProfileFactory.genProfile(user1);
    userProfileRepository.save(userProfile1);

    // Arrange: Create second user and user profile.
    clock.setFixedTime("2026-04-11T10:00:00Z");
    User user2 = userFactory.genRandUser(EnUserStatus.ACTIVE);
    UserProfile userProfile2 = userProfileFactory.genProfile(user2);
    userProfileRepository.save(userProfile2);

    // Arrange: request that uses already existing email.
    UserFullDataReq req = UserFullDataReq.builder().id(user1.getId()).email(user2.getEmail()).build();

    // Act: Try to change email to existing email.
    MvcResult mvcResult = mockMvc.perform(patch("/api/admin/user")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andReturn();

    // Assert: API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.CONFLICT.value());
    // Assert: Content has correct error.
    ProblemDetailBox expectedPdb = new ProblemDetailBox(
        HttpStatus.CONFLICT.value(),
        "User with given email already exists.",
        "Email '"+user2.getEmail()+"' already exists.",
        "/api/admin/user",
        "https://api.userland.org/errors/user/email/alreadyExists",
        Map.of("errCode", "user_0111")
    );
    problemDetailService.assertPd(mvcResult, expectedPdb);
  }
}
