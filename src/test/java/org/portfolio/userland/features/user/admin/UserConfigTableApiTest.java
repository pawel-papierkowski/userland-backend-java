package org.portfolio.userland.features.user.admin;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.portfolio.userland.common.dto.TableMetaReq;
import org.portfolio.userland.common.dto.TableMetaResp;
import org.portfolio.userland.common.services.table.TableHelper;
import org.portfolio.userland.features.user.BaseUserTest;
import org.portfolio.userland.features.user.dto.admin.config.UserConfigTableEntry;
import org.portfolio.userland.features.user.dto.admin.config.UserConfigTableReq;
import org.portfolio.userland.features.user.dto.admin.config.UserConfigTableResp;
import org.portfolio.userland.features.user.entities.EnUserStatus;
import org.portfolio.userland.features.user.entities.User;
import org.portfolio.userland.test.helpers.context.WithMockCustomUser;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

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
    userConfigFactory.genConfig(u00, "some.config.var", "Aa");
    userConfigFactory.genConfig(u00, "other.config.var", "BB");
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

    List<User> savedUsers = userRepository.saveAll(userList);
    return savedUsers;
  }

  /**
   * Act and assert table results.
   * @param req Request.
   * @param expectedEntries Expected entries.
   * @param pageCount Expected page count.
   * @param entryCount Expected entry count.
   * @throws Exception When objectMapper chokes on input.
   */
  private void actAssert(UserConfigTableReq req, List<UserConfigTableEntry> expectedEntries, Long pageCount, Long entryCount) throws Exception {
    // Act: Try to view table page with users.
    MvcResult mvcResult = mockMvc.perform(post("/api/admin/users/config")
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
  @WithMockCustomUser(authorities = { "ROLE_OPERATOR" })
  public void viewUserWithoutConfig() throws Exception {
    List<User> users = arrangeUserData();
    User userToCheck = users.get(1);

    // Act: get user with no config.
    UserConfigTableReq req = UserConfigTableReq.builder().userId(userToCheck.getId()).build();

    actAssert(req, List.of(), 0L, 0L);
  }

  @Test
  @WithMockCustomUser(authorities = { "ROLE_OPERATOR" })
  public void viewUserWithSingleConfig() throws Exception {
    List<User> users = arrangeUserData();
    User userToCheck = users.get(2);

    // Act: get user with single config.
    UserConfigTableReq req = UserConfigTableReq.builder().userId(userToCheck.getId()).build();
    List<UserConfigTableEntry> expectedResults = userAdminFactory.genUserConfigTableEntries(userToCheck.getConfigs());

    actAssert(req, expectedResults, 1L, 1L);
  }
}
