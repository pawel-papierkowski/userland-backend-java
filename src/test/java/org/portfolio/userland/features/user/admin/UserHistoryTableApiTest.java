package org.portfolio.userland.features.user.admin;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.portfolio.userland.common.dto.EnSortOrder;
import org.portfolio.userland.common.dto.TableMetaReq;
import org.portfolio.userland.common.dto.TableMetaResp;
import org.portfolio.userland.common.services.table.TableHelper;
import org.portfolio.userland.features.user.BaseUserTest;
import org.portfolio.userland.features.user.dto.admin.history.UserHistoryTableEntry;
import org.portfolio.userland.features.user.dto.admin.history.UserHistoryTableReq;
import org.portfolio.userland.features.user.dto.admin.history.UserHistoryTableResp;
import org.portfolio.userland.features.user.entities.EnUserHistoryWhat;
import org.portfolio.userland.features.user.entities.EnUserHistoryWho;
import org.portfolio.userland.features.user.entities.EnUserStatus;
import org.portfolio.userland.features.user.entities.User;
import org.portfolio.userland.test.helpers.context.WithMockCustomUser;
import org.portfolio.userland.test.helpers.problemDetail.ProblemDetailBox;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Integration test for user config table viewing.
 */
public class UserHistoryTableApiTest extends BaseUserTest {
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
    clock.setFixedTime("2026-06-10T11:00:00Z");
    userHistoryFactory.genHistoryEvent(u00, EnUserHistoryWho.USER, EnUserHistoryWhat.LOGIN, "");
    clock.setFixedTime("2026-06-10T12:00:00Z");
    userHistoryFactory.genHistoryEvent(u00, EnUserHistoryWho.USER, EnUserHistoryWhat.EDIT, "username");
    clock.setFixedTime("2026-06-10T13:00:00Z");
    userHistoryFactory.genHistoryEvent(u00, EnUserHistoryWho.USER, EnUserHistoryWhat.PASS_RESET_REQ, "");
    userList.add(u00);

    clock.setFixedTime("2026-06-09T15:00:00Z");
    User u01 = userFactory.genRandUser(EnUserStatus.ACTIVE);
    u01.setUsername("Aleksandra Kota");
    u01.setEmail("aleksandra.kota@example.com");
    u01.getHistory().clear();
    userList.add(u01);

    clock.setFixedTime("2026-06-09T14:00:00Z");
    User u02 = userFactory.genRandUser(EnUserStatus.PENDING);
    u02.setUsername("Roman Nowak");
    u02.setEmail("nadkonduktor@test.com");
    userList.add(u02);

    return userRepository.saveAll(userList);
  }

  /**
   * Act and assert user history results.
   * @param req Request.
   * @param expectedEntries Expected entries.
   * @param pageCount Expected page count.
   * @param entryCount Expected entry count.
   * @throws Exception When objectMapper chokes on input.
   */
  private void actAssert(UserHistoryTableReq req, List<UserHistoryTableEntry> expectedEntries, Long pageCount, Long entryCount) throws Exception {
    // Act: Try to view table page with user history.
    MvcResult mvcResult = mockMvc.perform(post("/api/admin/user/history")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andReturn();

    // Assert: API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.OK.value());

    // Assert: All results from given page returned.
    UserHistoryTableResp actualResp = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), UserHistoryTableResp.class);

    TableMetaReq tableMetaReq = TableHelper.prepareTableMeta(req.tableMeta());
    TableMetaResp expectedTableMetaResp = TableMetaResp.builder()
        .pageCount(pageCount)
        .entryCount(entryCount)
        .pageSize(tableMetaReq.pageSize())
        .page(tableMetaReq.page())
        .sortBy(tableMetaReq.sortBy())
        .sortOrder(tableMetaReq.sortOrder())
        .build();

    UserHistoryTableResp expectedResp = new UserHistoryTableResp(expectedEntries, expectedTableMetaResp);
    userAdminAssert.assertUserHistoryPage(actualResp, expectedResp);
  }

  //

  @Test
  @WithMockCustomUser(authorities = { "ROLE_OPERATOR", "USER_VIEW" })
  public void viewNonexistentUser() throws Exception {
    List<User> users = arrangeUserData();
    User userToCheck = users.get(2);

    // Act: get nonexistent user.
    UserHistoryTableReq req = UserHistoryTableReq.builder().userId(userToCheck.getId()+1).build();

    actAssert(req, List.of(), 0L, 0L);
  }

  @Test
  @WithMockCustomUser(authorities = { "ROLE_OPERATOR", "USER_VIEW" })
  public void viewUserWithoutHistory() throws Exception {
    List<User> users = arrangeUserData();
    User userToCheck = users.get(1);

    // Act: get user with no history events.
    UserHistoryTableReq req = UserHistoryTableReq.builder().userId(userToCheck.getId()).build();

    actAssert(req, List.of(), 0L, 0L);
  }

  @Test
  @WithMockCustomUser(authorities = { "ROLE_OPERATOR", "USER_VIEW" })
  public void viewUserWithSingleHistory() throws Exception {
    List<User> users = arrangeUserData();
    User userToCheck = users.get(2);

    // Act: get user with single history event.
    UserHistoryTableReq req = UserHistoryTableReq.builder().userId(userToCheck.getId()).build();
    List<UserHistoryTableEntry> expectedResults = userAdminFactory.genUserHistoryTableEntries(userToCheck.getHistory());

    actAssert(req, expectedResults, 1L, 1L);
  }

  @Test
  @WithMockCustomUser(authorities = { "ROLE_OPERATOR", "USER_VIEW" })
  public void viewUserWithManyHistories() throws Exception {
    List<User> users = arrangeUserData();
    User userToCheck = users.getFirst();

    // Act: get user with many history events.
    UserHistoryTableReq req = UserHistoryTableReq.builder()
        .userId(userToCheck.getId())
        .tableMeta(TableMetaReq.builder().sortBy("createdAt").sortOrder(EnSortOrder.ASC).build())
        .build();
    List<UserHistoryTableEntry> expectedResults = userAdminFactory.genUserHistoryTableEntries(userToCheck.getHistory());

    actAssert(req, expectedResults, 1L, 5L);
  }

  // //////////////////////////////////////////////////////////////////////////
  // FAILURES

  @Test
  @WithMockCustomUser(authorities = { "ROLE_OPERATOR" }) // missing USER_VIEW
  public void viewWithoutPermissions() throws Exception {
    // Tests verification: needs correct permissions.
    List<User> users = arrangeUserData();
    User userToCheck = users.get(2);

    UserHistoryTableReq req = UserHistoryTableReq.builder().userId(userToCheck.getId()).build();

    // Act: Try to view table page with user history.
    MvcResult mvcResult = mockMvc.perform(post("/api/admin/user/history")
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
        "/api/admin/user/history",
        "https://api.general.org/errors/forbidden",
        Map.of()
    );
    problemDetailService.assertPd(mvcResult, expectedPdb);
  }
}
