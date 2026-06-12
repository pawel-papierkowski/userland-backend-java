package org.portfolio.userland.features.user.admin;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.portfolio.userland.common.dto.*;
import org.portfolio.userland.common.services.table.TableHelper;
import org.portfolio.userland.features.user.BaseUserTest;
import org.portfolio.userland.features.user.constants.UserErrCode;
import org.portfolio.userland.features.user.dto.admin.config.UserConfigEditReq;
import org.portfolio.userland.features.user.dto.admin.config.UserConfigTableEntry;
import org.portfolio.userland.features.user.dto.admin.config.UserConfigTableReq;
import org.portfolio.userland.features.user.dto.admin.config.UserConfigTableResp;
import org.portfolio.userland.features.user.entities.EnUserStatus;
import org.portfolio.userland.features.user.entities.User;
import org.portfolio.userland.features.user.entities.UserConfig;
import org.portfolio.userland.system.auth.details.CustomUserDetails;
import org.portfolio.userland.test.helpers.context.WithMockCustomUser;
import org.portfolio.userland.test.helpers.problemDetail.ProblemDetailBox;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/**
 * Integration test for user config table viewing.
 */
public class UserConfigTableApiTest extends BaseUserTest {
  @BeforeEach
  public void tearDown() {
    resetDatabase();
  }

  // //////////////////////////////////////////////////////////////////////////

  /** Create user data for testing. */
  private List<User> arrangeUserData() {
    List<User> userList = new ArrayList<>();

    clock.setFixedTime("2026-06-10T10:00:00Z");
    User u00 = userFactory.genRandUser(EnUserStatus.ACTIVE);
    u00.setUsername("Jan Kowalski");
    u00.setEmail("jan.kowalski@test.com");
    clock.setFixedTime("2026-06-10T13:00:00Z");
    userConfigFactory.genConfig(u00, "other.config.var", "Aa");
    clock.setFixedTime("2026-06-10T12:00:00Z");
    userConfigFactory.genConfig(u00, "some.config.var", "cc");
    clock.setFixedTime("2026-06-10T11:00:00Z");
    userConfigFactory.genConfig(u00, "another.config.var", "BB");
    userList.add(u00);

    clock.setFixedTime("2026-06-09T15:00:00Z");
    User u01 = userFactory.genRandUser(EnUserStatus.ACTIVE);
    u01.setUsername("Aleksandra Kota");
    u01.setEmail("aleksandra.kota@example.com");
    userList.add(u01);

    clock.setFixedTime("2026-06-09T14:00:00Z");
    User u02 = userFactory.genRandUser(EnUserStatus.ACTIVE);
    u02.setUsername("Roman Nowak");
    u02.setEmail("nadkonduktor@test.com");
    userConfigFactory.genConfig(u02, "jwt.expires", "1440");
    userList.add(u02);

    return userRepository.saveAll(userList);
  }

  /**
   * Act and assert user config results.
   * @param req Request.
   * @param expectedEntries Expected entries.
   * @param pageCount Expected page count.
   * @param entryCount Expected entry count.
   * @throws Exception When objectMapper chokes on input.
   */
  private void actAssertView(UserConfigTableReq req, List<UserConfigTableEntry> expectedEntries, Long pageCount, Long entryCount) throws Exception {
    // Act: Try to view table page with user configs.
    MvcResult mvcResult = mockMvc.perform(post("/api/admin/user/configs")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andReturn();

    // Assert: API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.OK.value());

    // Assert: All results from given page returned.
    UserConfigTableResp actualResp = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), UserConfigTableResp.class);

    TableMetaReq tableMetaReq = TableHelper.prepareTableMeta(req.tableMeta());
    TableMetaResp expectedTableMetaResp = TableMetaResp.builder()
        .pageCount(pageCount)
        .entryCount(entryCount)
        .pageSize(tableMetaReq.pageSize())
        .page(tableMetaReq.page())
        .sortBy(tableMetaReq.sortBy())
        .sortOrder(tableMetaReq.sortOrder())
        .build();

    UserConfigTableResp expectedResp = new UserConfigTableResp(expectedEntries, expectedTableMetaResp);
    userAdminAssert.assertUserConfigPage(actualResp, expectedResp);
  }

  //

  @Test
  @WithMockCustomUser(authorities = { "ROLE_OPERATOR", "USER_VIEW" })
  public void viewNonexistentUser() throws Exception {
    List<User> users = arrangeUserData();
    User userToCheck = users.getLast();

    // Act: get nonexistent user.
    UserConfigTableReq req = UserConfigTableReq.builder().userId(userToCheck.getId()+1).build();

    actAssertView(req, List.of(), 0L, 0L);
  }

  @Test
  @WithMockCustomUser(authorities = { "ROLE_OPERATOR", "USER_VIEW" })
  public void viewUserWithoutConfig() throws Exception {
    List<User> users = arrangeUserData();
    User userToCheck = users.get(1);

    // Act: get user with no config entries.
    UserConfigTableReq req = UserConfigTableReq.builder().userId(userToCheck.getId()).build();

    actAssertView(req, List.of(), 0L, 0L);
  }

  @Test
  @WithMockCustomUser(authorities = { "ROLE_OPERATOR", "USER_VIEW" })
  public void viewUserWithSingleConfig() throws Exception {
    List<User> users = arrangeUserData();
    User userToCheck = users.get(2);

    // Act: get user with single config entry.
    UserConfigTableReq req = UserConfigTableReq.builder().userId(userToCheck.getId()).build();
    EntryMetaResp meta = EntryMetaResp.builder()
        .options(Map.of("edit", EntryOption.builder().access(EnOptionAccess.DISABLED).reason("adminOnly").build(),
            "delete", EntryOption.builder().access(EnOptionAccess.DISABLED).reason("adminOnly").build()))
        .build();
    List<UserConfigTableEntry> expectedResults = userAdminFactory.genUserConfigTableEntries(userToCheck.getConfigs(), meta);

    actAssertView(req, expectedResults, 1L, 1L);
  }

  @Test
  @WithMockCustomUser(authorities = { "ROLE_ADMIN" })
  public void viewUserWithSingleConfigAsAdmin() throws Exception {
    List<User> users = arrangeUserData();
    User userToCheck = users.get(2);

    // Act: get user with single config entry as admin. Result will show I am allowed to alter config entry.
    UserConfigTableReq req = UserConfigTableReq.builder().userId(userToCheck.getId()).build();
    EntryMetaResp meta = EntryMetaResp.builder()
        .options(Map.of("edit", EntryOption.builder().access(EnOptionAccess.ENABLED).reason(null).build(),
            "delete", EntryOption.builder().access(EnOptionAccess.ENABLED).reason(null).build()))
        .build();
    List<UserConfigTableEntry> expectedResults = userAdminFactory.genUserConfigTableEntries(userToCheck.getConfigs(), meta);

    actAssertView(req, expectedResults, 1L, 1L);
  }

  @Test
  @WithMockCustomUser(authorities = { "ROLE_OPERATOR", "USER_VIEW" })
  public void viewUserWithManyConfigs() throws Exception {
    List<User> users = arrangeUserData();
    User userToCheck = users.getFirst();

    // Act: get user with many config entries.
    UserConfigTableReq req = UserConfigTableReq.builder()
        .userId(userToCheck.getId())
        .tableMeta(TableMetaReq.builder().sortBy("name").sortOrder(EnSortOrder.DESC).build())
        .build();
    EntryMetaResp meta = EntryMetaResp.builder()
        .options(Map.of("edit", EntryOption.builder().access(EnOptionAccess.DISABLED).reason("adminOnly").build(),
            "delete", EntryOption.builder().access(EnOptionAccess.DISABLED).reason("adminOnly").build()))
        .build();
    List<UserConfigTableEntry> configResults = userAdminFactory.genUserConfigTableEntries(userToCheck.getConfigs(), meta);
    List<UserConfigTableEntry> expectedResults = List.of(configResults.get(1), configResults.get(0), configResults.get(2));

    actAssertView(req, expectedResults, 1L, 3L);
  }

  //

  @Test
  @WithMockCustomUser(authorities = { "ROLE_ADMIN" })
  public void addUserConfig() throws Exception {
    clock.setFixedTime("2026-06-11T10:00:00Z");
    // Arrange: Get user with many config entries.
    List<User> users = arrangeUserData();
    User user = users.getFirst();

    // Arrange: Prepare request.
    UserConfigEditReq req = UserConfigEditReq.builder()
        .id(null) // add new entry
        .userId(user.getId())
        .name("new.config")
        .value("new.value")
        .build();

    // Act: Try to add new user config entry.
    MvcResult mvcResult = mockMvc.perform(patch("/api/admin/user/config")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andReturn();

    // Assert: API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.OK.value());

    // Assert: Database state.
    transactionTemplate.execute(_ -> {
      User expectedUser = users.getFirst();
      userConfigFactory.genConfig(expectedUser, "new.config", "new.value");

      // Assert: User state.
      assertAllUser(user.getEmail(), expectedUser, null);
      return null;
    });
  }

  //

  @Test
  @WithMockCustomUser(authorities = { "ROLE_ADMIN" })
  public void editUserConfig() throws Exception {
    // Arrange: Get user with many config entries.
    List<User> users = arrangeUserData();
    User user = users.getFirst();
    UserConfig userConfig = user.getConfigs().getFirst();

    // Arrange: Prepare request.
    UserConfigEditReq req = UserConfigEditReq.builder()
        .id(userConfig.getId())
        .userId(user.getId())
        .name("new.config")
        .value("new.value")
        .build();

    // Act: Try to change existing user config entry.
    MvcResult mvcResult = mockMvc.perform(patch("/api/admin/user/config")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andReturn();

    // Assert: API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.OK.value());

    // Assert: Database state.
    transactionTemplate.execute(_ -> {
      User expectedUser = users.getFirst();
      UserConfig expectedUserConfig = user.getConfigs().getFirst();
      expectedUserConfig.setName("new.config");
      expectedUserConfig.setValue("new.value");

      // Assert: User state.
      assertAllUser(user.getEmail(), expectedUser, null);
      return null;
    });
  }

  @Test
  @WithMockCustomUser(authorities = { "ROLE_ADMIN" })
  public void editSameUserConfig() throws Exception {
    // Changing user config into same user config is allowed, if pointless.

    // Arrange: Get user with many config entries.
    List<User> users = arrangeUserData();
    User user = users.getFirst();
    UserConfig userConfig = user.getConfigs().getFirst();

    // Arrange: Prepare request.
    UserConfigEditReq req = UserConfigEditReq.builder()
        .id(userConfig.getId())
        .userId(user.getId())
        .name("other.config.var")
        .value("Aa")
        .build();

    // Act: Try to change existing user config entry.
    MvcResult mvcResult = mockMvc.perform(patch("/api/admin/user/config")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andReturn();

    // Assert: API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.OK.value());

    // Assert: Database state.
    transactionTemplate.execute(_ -> {
      User expectedUser = users.getFirst();

      // Assert: User state.
      assertAllUser(user.getEmail(), expectedUser, null);
      return null;
    });
  }

  //

  @Test
  @WithMockCustomUser(authorities = { "ROLE_ADMIN" })
  public void deleteUserConfig() throws Exception {
    // Arrange: Get user with many config entries.
    List<User> users = arrangeUserData();
    User user = users.getFirst();
    UserConfig userConfig = user.getConfigs().getFirst();

    // Act: Try to remove user config entry.
    MvcResult mvcResult = mockMvc.perform(delete("/api/admin/user/config/"+userConfig.getId()))
        .andReturn();

    // Assert: API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.OK.value());

    // Assert: Database state.
    transactionTemplate.execute(_ -> {
      User expectedUser = users.getFirst();
      expectedUser.getConfigs().removeFirst();

      // Assert: User state.
      assertAllUser(user.getEmail(), expectedUser, null);
      return null;
    });
  }

  // //////////////////////////////////////////////////////////////////////////
  // FAILURES

  @Test
  @WithMockCustomUser(authorities = { "ROLE_OPERATOR" }) // missing USER_VIEW
  public void viewWithoutPermissions() throws Exception {
    // Tests verification: needs correct permissions.
    List<User> users = arrangeUserData();
    User userToCheck = users.get(2);

    UserConfigTableReq req = UserConfigTableReq.builder().userId(userToCheck.getId()).build();

    // Act: Try to view table page with user history.
    MvcResult mvcResult = mockMvc.perform(post("/api/admin/user/configs")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andReturn();

    // Assert: API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.FORBIDDEN.value());
    // Assert: Content has correct error.
    ProblemDetailBox expectedPdb = new ProblemDetailBox(
        HttpStatus.FORBIDDEN.value(),
        "Forbidden",
        "You do not have permission to access this resource.",
        "/api/admin/user/configs",
        "https://api.general.org/errors/forbidden",
        Map.of()
    );
    problemDetailService.assertPd(mvcResult, expectedPdb);
  }

  //

  @Test
  @WithMockCustomUser(authorities = { "ROLE_OPERATOR", "USER_EDIT" }) // must be ROLE_ADMIN
  public void addUserConfigWithoutPermission() throws Exception {
    clock.setFixedTime("2026-06-11T10:00:00Z");
    // Arrange: Get user with many config entries.
    List<User> users = arrangeUserData();
    User user = users.getFirst();

    // Arrange: Prepare request.
    UserConfigEditReq req = UserConfigEditReq.builder()
        .id(null) // add new entry
        .userId(user.getId())
        .name("new.config")
        .value("new.value")
        .build();

    // Act: Try to add new user config entry.
    MvcResult mvcResult = mockMvc.perform(patch("/api/admin/user/config")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andReturn();

    // Assert: API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.FORBIDDEN.value());
    // Assert: Content has correct error.
    ProblemDetailBox expectedPdb = new ProblemDetailBox(
        HttpStatus.FORBIDDEN.value(),
        "Forbidden",
        "You do not have permission to access this resource.",
        "/api/admin/user/config",
        "https://api.general.org/errors/forbidden",
        Map.of()
    );
    problemDetailService.assertPd(mvcResult, expectedPdb);
  }

  @Test
  @WithMockCustomUser(authorities = { "ROLE_ADMIN" })
  public void addAlreadyExistingUserConfig() throws Exception {
    clock.setFixedTime("2026-06-11T10:00:00Z");
    // Arrange: Get user with many config entries.
    List<User> users = arrangeUserData();
    User user = users.getFirst();

    // Arrange: Prepare request.
    UserConfigEditReq req = UserConfigEditReq.builder()
        .id(null) // add new entry
        .userId(user.getId())
        .name("some.config.var")
        .value("this name already exists")
        .build();

    // Act: Try to add new user config entry.
    MvcResult mvcResult = mockMvc.perform(patch("/api/admin/user/config")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andReturn();

    // Assert: API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.CONFLICT.value());
    // Assert: Content has correct error.
    ProblemDetailBox expectedPdb = new ProblemDetailBox(
        HttpStatus.CONFLICT.value(),
        "User config entry is redundant.",
        "User config entry 'some.config.var' already exists.",
        "/api/admin/user/config",
        "https://api.userland.org/errors/user/config/redundant",
        Map.of("errCode", UserErrCode.CONFIG_REDUNDANT)
    );
    problemDetailService.assertPd(mvcResult, expectedPdb);
  }

  //

  @Test
  @WithMockCustomUser(authorities = { "ROLE_OPERATOR", "USER_EDIT" }) // must be ROLE_ADMIN
  public void editUserConfigWithoutPermission() throws Exception {
    // Arrange: Get user with many config entries.
    List<User> users = arrangeUserData();
    User user = users.getFirst();
    UserConfig userConfig = user.getConfigs().getFirst();

    // Arrange: Prepare request.
    UserConfigEditReq req = UserConfigEditReq.builder()
        .id(userConfig.getId())
        .userId(user.getId())
        .name("new.config")
        .value("new.value")
        .build();

    // Act: Try to change existing user config entry.
    MvcResult mvcResult = mockMvc.perform(patch("/api/admin/user/config")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andReturn();

    // Assert: API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.FORBIDDEN.value());
    // Assert: Content has correct error.
    ProblemDetailBox expectedPdb = new ProblemDetailBox(
        HttpStatus.FORBIDDEN.value(),
        "Forbidden",
        "You do not have permission to access this resource.",
        "/api/admin/user/config",
        "https://api.general.org/errors/forbidden",
        Map.of()
    );
    problemDetailService.assertPd(mvcResult, expectedPdb);
  }

  @Test
  public void editYourOwnUserConfig() throws Exception {
    // Arrange: Get user with many config entries.
    List<User> users = arrangeUserData();
    User user = users.getFirst();
    UserConfig userConfig = user.getConfigs().getFirst();

    // Arrange: Prepare request.
    UserConfigEditReq req = UserConfigEditReq.builder()
        .id(userConfig.getId())
        .userId(user.getId())
        .name("new.config")
        .value("new.value")
        .build();

    // Arrange: We want to emulate fact of this very user being logged in. We can't use @WithMockCustomUser as we do not
    // know user's id in advance.
    CustomUserDetails customUserDetails = new CustomUserDetails(user.getId(), true, false, user.getUsername(), user.getEmail(), "", Set.of(),
        List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

    // Act: Try to change existing user config entry.
    MvcResult mvcResult = mockMvc.perform(patch("/api/admin/user/config")
            .with(user(customUserDetails))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andReturn();

    // Assert: API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.CONFLICT.value());
    // Assert: Content has correct error.
    ProblemDetailBox expectedPdb = new ProblemDetailBox(
        HttpStatus.CONFLICT.value(),
        "Not allowed to edit this user.",
        "User with id '"+user.getId()+"' cannot be edited.",
        "/api/admin/user/config",
        "https://api.userland.org/errors/user/cannotEdit",
        Map.of("errCode", UserErrCode.CANNOT_EDIT)
    );
    problemDetailService.assertPd(mvcResult, expectedPdb);
  }

  //

  @Test
  @WithMockCustomUser(authorities = { "ROLE_OPERATOR", "USER_EDIT" }) // must be ROLE_ADMIN
  public void deleteUserConfigWithoutPermission() throws Exception {
    // Arrange: Get user with many config entries.
    List<User> users = arrangeUserData();
    User user = users.getFirst();
    UserConfig userConfig = user.getConfigs().getFirst();

    // Act: Try to remove user config entry.
    MvcResult mvcResult = mockMvc.perform(delete("/api/admin/user/config/"+userConfig.getId()))
        .andReturn();

    // Assert: API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.FORBIDDEN.value());
    // Assert: Content has correct error.
    ProblemDetailBox expectedPdb = new ProblemDetailBox(
        HttpStatus.FORBIDDEN.value(),
        "Forbidden",
        "You do not have permission to access this resource.",
        "/api/admin/user/config/"+userConfig.getId(),
        "https://api.general.org/errors/forbidden",
        Map.of()
    );
    problemDetailService.assertPd(mvcResult, expectedPdb);
  }

  @Test
  public void deleteYourOwnUserConfig() throws Exception {
    // Arrange: Get user with many config entries.
    List<User> users = arrangeUserData();
    User user = users.getFirst();
    UserConfig userConfig = user.getConfigs().getFirst();

    // Arrange: We want to emulate fact of this very user being logged in. We can't use @WithMockCustomUser as we do not
    // know user's id in advance.
    CustomUserDetails customUserDetails = new CustomUserDetails(user.getId(), true, false, user.getUsername(), user.getEmail(), "", Set.of(),
        List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

    // Act: Try to remove user config entry.
    MvcResult mvcResult = mockMvc.perform(delete("/api/admin/user/config/"+userConfig.getId())
            .with(user(customUserDetails)))
        .andReturn();

    // Assert: API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.CONFLICT.value());
    // Assert: Content has correct error.
    ProblemDetailBox expectedPdb = new ProblemDetailBox(
        HttpStatus.CONFLICT.value(),
        "Not allowed to edit this user.",
        "User with id '"+user.getId()+"' cannot be edited.",
        "/api/admin/user/config/"+userConfig.getId(),
        "https://api.userland.org/errors/user/cannotEdit",
        Map.of("errCode", UserErrCode.CANNOT_EDIT)
    );
    problemDetailService.assertPd(mvcResult, expectedPdb);
  }
}
