package org.portfolio.userland.features.user.standard;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.portfolio.userland.features.user.BaseUserTest;
import org.portfolio.userland.features.user.constants.UserErrCode;
import org.portfolio.userland.features.user.dto.common.UserDataResp;
import org.portfolio.userland.features.user.dto.common.UserProfileData;
import org.portfolio.userland.features.user.dto.standard.edit.UserEditReq;
import org.portfolio.userland.features.user.entities.*;
import org.portfolio.userland.test.helpers.context.WithMockCustomUser;
import org.portfolio.userland.test.helpers.problemDetail.ProblemDetailBox;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
        .version(0L)
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
    UserDataResp expectedResp = UserDataResp.builder().version(0L).username("Jane").email("test@example.com").lang("en").profile(expectedProfile).build();
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
        .version(0L)
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
    UserDataResp expectedResp = UserDataResp.builder().version(1L).username("JasiuFasola44").email("test@example.com").lang("pl").profile(expectedProfile).build();
    assertThat(actualResp).as("User data is invalid").usingRecursiveComparison().isEqualTo(expectedResp);

    // Prepare expected result.
    expectedUser.setModifiedAt(clockService.getNowUTC());
    expectedUser.setVersion(1L);
    expectedUser.setUsername("JasiuFasola44");
    expectedUser.setLang("pl");
    expectedUserProfile.setName("Jasiu");
    expectedUserProfile.setSurname("Fasola");
    expectedUserProfile.setVersion(1L);
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
        .version(0L)
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
    UserDataResp expectedResp = UserDataResp.builder().version(1L).username("Robert").email("test@example.com").lang("en").profile(expectedProfile).build();
    assertThat(actualResp).as("User data is invalid").usingRecursiveComparison().isEqualTo(expectedResp);

    // Prepare expected result.
    expectedUser.setModifiedAt(clockService.getNowUTC());
    expectedUser.setVersion(1L);
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
        .version(0L)
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
    UserDataResp expectedResp = UserDataResp.builder().version(1L).username("Jane").email("test@example.com").lang("pl").profile(expectedProfile).build();
    assertThat(actualResp).as("User data is invalid").usingRecursiveComparison().isEqualTo(expectedResp);

    // Prepare expected result.
    expectedUser.setModifiedAt(clockService.getNowUTC());
    expectedUser.setVersion(1L);
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
  public void editUserProfileName() throws Exception {
    // Edit user: change only name field in profile.
    clock.setFixedTime("2026-04-10T10:00:00Z");

    // Arrange: Create active user and profile.
    User expectedUser = userFactory.genUser(EnUserStatus.ACTIVE);
    UserProfile expectedUserProfile = userProfileFactory.genRandProfile(expectedUser);
    userProfileRepository.save(expectedUserProfile);

    clock.setFixedTime("2026-04-10T10:05:00Z");

    // Arrange: Create request for editing of user account.
    UserProfileData profileReq = UserProfileData.builder().name("Tom").build();
    UserEditReq req = UserEditReq.builder()
        .version(0L).profile(profileReq).build();

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
    UserDataResp expectedResp = UserDataResp.builder().version(1L).username("Jane").email("test@example.com").lang("en").profile(expectedProfile).build();
    assertThat(actualResp).as("User data is invalid").usingRecursiveComparison().isEqualTo(expectedResp);

    // Prepare expected result.
    expectedUser.setModifiedAt(clockService.getNowUTC());
    expectedUser.setVersion(1L);
    expectedUserProfile.setName("Tom");
    expectedUserProfile.setVersion(1L);
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
  public void editUserProfileSurname() throws Exception {
    // Edit user: change only surname field in profile.
    clock.setFixedTime("2026-04-10T10:00:00Z");

    // Arrange: Create active user and profile.
    User expectedUser = userFactory.genUser(EnUserStatus.ACTIVE);
    UserProfile expectedUserProfile = userProfileFactory.genRandProfile(expectedUser);
    userProfileRepository.save(expectedUserProfile);

    clock.setFixedTime("2026-04-10T10:05:00Z");

    // Arrange: Create request for editing of user account.
    UserProfileData profileReq = UserProfileData.builder().surname("Bombadil").build();
    UserEditReq req = UserEditReq.builder()
        .version(0L).profile(profileReq).build();

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
    expectedProfile = expectedProfile.toBuilder().surname("Bombadil").build(); // only one field changed
    UserDataResp expectedResp = UserDataResp.builder().version(1L).username("Jane").email("test@example.com").lang("en").profile(expectedProfile).build();
    assertThat(actualResp).as("User data is invalid").usingRecursiveComparison().isEqualTo(expectedResp);

    // Prepare expected result.
    expectedUser.setModifiedAt(clockService.getNowUTC());
    expectedUser.setVersion(1L);
    expectedUserProfile.setSurname("Bombadil");
    expectedUserProfile.setVersion(1L);
    userHistoryFactory.genHistoryEvent(expectedUser, EnUserHistoryWho.USER, EnUserHistoryWhat.EDIT, "surname");

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
    UserEditReq req = UserEditReq.builder()
        .version(0L).build();

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
    UserDataResp expectedResp = UserDataResp.builder().version(0L).username("Jane").email("test@example.com").lang("en").profile(expectedProfile).build();
    assertThat(actualResp).as("User data is invalid").usingRecursiveComparison().isEqualTo(expectedResp);

    // Assert: Database state.
    transactionTemplate.execute(_ -> {
      // Assert: User state.
      User user = assertAllUser("test@example.com", expectedUser, expectedUserProfile);
      assertThat(user.getPassword()).as("Password hash should be same").isEqualTo(expectedUser.getPassword());
      return null;
    });
  }

  //

  @Test
  @WithMockCustomUser
  public void editUserStaleVersion() throws Exception {
    // Edit user: version sent by client does not match, as someone else modified data in the meantime.
    clock.setFixedTime("2026-04-10T10:00:00Z");

    // Arrange: Create active user and profile.
    User expectedUser = userFactory.genUser(EnUserStatus.ACTIVE);
    UserProfile expectedUserProfile = userProfileFactory.genRandProfile(expectedUser);
    userProfileRepository.save(expectedUserProfile);

    // Arrange: Simulate concurrent modification done by someone else after client read the data (version bumped to 1).
    transactionTemplate.execute(_ -> {
      User concurrentUser = userRepository.findByEmail("test@example.com").orElseThrow();
      concurrentUser.setUsername("SomeoneElse");
      userRepository.save(concurrentUser);
      return null;
    });

    clock.setFixedTime("2026-04-10T10:05:00Z");

    // Arrange: Create request for editing of user account with stale version.
    UserProfileData profileReq = UserProfileData.builder().build();
    UserEditReq req = UserEditReq.builder()
        .version(0L) // stale, actual version is 1
        .username("Jane")
        .profile(profileReq)
        .build();

    // Act: Try to edit user account.
    MvcResult mvcResult = mockMvc.perform(patch("/api/users/edit")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andReturn();

    // Assert: API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.CONFLICT.value());
    // Assert: Content has correct error.
    ProblemDetailBox expectedPdb = new ProblemDetailBox(
        HttpStatus.CONFLICT.value(),
        "Data was modified in the meantime.",
        "User with id '" + expectedUser.getId() + "' was modified by someone else. Please reload data and try again.",
        "/api/users/edit",
        "https://api.userland.org/errors/user/dataStale",
        Map.of("errCode", UserErrCode.DATA_STALE)
    );
    problemDetailService.assertPd(mvcResult, expectedPdb);

    // Assert: Database state - username of concurrent modification must be kept, our change must be rejected.
    transactionTemplate.execute(_ -> {
      User user = userRepository.findByEmail("test@example.com").orElseThrow();
      assertThat(user.getUsername()).as("Username should not be changed").isEqualTo("SomeoneElse");
      assertThat(user.getVersion()).as("Version should stay on value from concurrent modification").isEqualTo(1L);
      return null;
    });
  }

  @Test
  @WithMockCustomUser
  public void editUserMissingVersion() throws Exception {
    // Edit user: version is missing entirely - request must fail validation.
    clock.setFixedTime("2026-04-10T10:00:00Z");

    // Arrange: Create active user and profile.
    User expectedUser = userFactory.genUser(EnUserStatus.ACTIVE);
    UserProfile expectedUserProfile = userProfileFactory.genRandProfile(expectedUser);
    userProfileRepository.save(expectedUserProfile);

    // Arrange: Create request without version field.
    UserEditReq req = UserEditReq.builder()
        .username("Jane")
        .build();

    // Act: Try to edit user account.
    MvcResult mvcResult = mockMvc.perform(patch("/api/users/edit")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andReturn();

    // Assert: API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.BAD_REQUEST.value());
  }

  @Test
  @WithMockCustomUser
  public void editUserConcurrentFlushConflict() throws Exception {
    // Verify DB-level protection: saving entity with stale version fails with optimistic locking exception at flush.
    clock.setFixedTime("2026-04-10T10:00:00Z");

    // Arrange: Create active user and profile.
    User expectedUser = userFactory.genUser(EnUserStatus.ACTIVE);
    UserProfile expectedUserProfile = userProfileFactory.genRandProfile(expectedUser);
    userProfileRepository.save(expectedUserProfile);

    // Arrange: Detached copy of user with old version (0).
    User staleUser = userRepository.findByEmail("test@example.com").orElseThrow();

    // Arrange: Concurrent modification bumps version to 1.
    transactionTemplate.execute(_ -> {
      User concurrentUser = userRepository.findByEmail("test@example.com").orElseThrow();
      concurrentUser.setUsername("SomeoneElse");
      userRepository.save(concurrentUser);
      return null;
    });

    // Act + Assert: Try to save stale detached copy - must fail with optimistic locking failure.
    assertThatThrownBy(() -> transactionTemplate.execute(_ -> {
      staleUser.setUsername("TooLate");
      userRepository.save(staleUser);
      return null;
    }))
        .as("Saving entity with stale version should fail")
        .isInstanceOf(OptimisticLockingFailureException.class);
  }
}
