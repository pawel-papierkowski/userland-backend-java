package org.portfolio.userland.features.user.admin;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.portfolio.userland.common.dto.EnSortOrder;
import org.portfolio.userland.common.dto.TableMeta;
import org.portfolio.userland.features.user.BaseUserTest;
import org.portfolio.userland.features.user.dto.admin.view.UserPageResp;
import org.portfolio.userland.features.user.dto.admin.view.UserTableEntry;
import org.portfolio.userland.features.user.dto.admin.view.UserTableViewReq;
import org.portfolio.userland.features.user.entities.EnUserStatus;
import org.portfolio.userland.features.user.entities.User;
import org.portfolio.userland.test.helpers.context.WithMockCustomUser;
import org.portfolio.userland.test.helpers.problemDetail.ProblemDetailBox;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Integration test for user table viewing.
 */
public class UserTableApiTest extends BaseUserTest {
  @AfterEach
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
    userList.add(u00);

    clock.setFixedTime("2026-06-09T15:00:00Z");
    User u01 = userFactory.genRandUser(EnUserStatus.ACTIVE);
    u01.setUsername("Aleksandra Kota");
    u01.setEmail("aleksandra.kota@example.com");
    userList.add(u01);
    clock.setFixedTime("2026-06-09T14:00:00Z");
    User u02 = userFactory.genRandUser(EnUserStatus.PENDING);
    u02.setUsername("Roman Nowak");
    u02.setEmail("nadkonduktor@test.com");
    userList.add(u02);
    clock.setFixedTime("2026-06-09T13:00:00Z");
    User u03 = userFactory.genRandUser(EnUserStatus.ACTIVE);
    u03.setUsername("Alma");
    u03.setEmail("alma22@test.com");
    userList.add(u03);
    clock.setFixedTime("2026-06-09T12:00:00Z");
    User u04 = userFactory.genRandUser(EnUserStatus.PENDING);
    u04.setUsername("CatZilla");
    u04.setEmail("enumerator@example.com");
    userList.add(u04);

    clock.setFixedTime("2026-06-08T15:00:00Z");
    User u05 = userFactory.genRandUser(EnUserStatus.ACTIVE);
    u05.setUsername("Shrek");
    u05.setEmail("shrek@test.com");
    u05.setLocked(true);
    userList.add(u05);
    clock.setFixedTime("2026-06-07T05:45:00Z");
    User u06 = userFactory.genRandUser(EnUserStatus.ACTIVE);
    u06.setUsername("Kunkator");
    u06.setEmail("kawaii@example.com");
    userList.add(u06);

    userRepository.saveAll(userList);
    return userList;
  }

  /**
   * Act and assert results.
   * @param req Request.
   * @param expectedResults Expected results.
   * @throws Exception When objectMapper chokes on input.
   */
  private void actAssert(UserTableViewReq req, List<UserTableEntry> expectedResults) throws Exception {
    // Act: Try to view table page with users.
    MvcResult mvcResult = mockMvc.perform(post("/api/admin/users")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andReturn();

    // Assert: API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.OK.value());
    // Assert: All results from first page returned.
    UserPageResp actualResp = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), UserPageResp.class);
    UserPageResp expectedResp = new UserPageResp(expectedResults);
    userAdminAssert.assertUserPage(actualResp, expectedResp);
  }

  //

  @Test
  @WithMockCustomUser(authorities = { "ROLE_OPERATOR" })
  public void viewEmptyTable() throws Exception {
    // Arrange: all defaults.
    UserTableViewReq req = UserTableViewReq.builder().build();

    actAssert(req, List.of());
  }

  //

  @Test
  @WithMockCustomUser(authorities = { "ROLE_OPERATOR" })
  public void viewUsername() throws Exception {
    // Tests getting only one result and username filtering.
    List<User> users = arrangeUserData();
    List<UserTableEntry> userResults = userAdminFactory.genUserTableEntries(users);
    List<UserTableEntry> expectedResults = List.of(userResults.getFirst());

    UserTableViewReq req = UserTableViewReq.builder()
        .username("Jan Kow") // will match first user "Jan Kowalski"
        .build();

    actAssert(req, expectedResults);
  }

  @Test
  @WithMockCustomUser(authorities = { "ROLE_OPERATOR" })
  public void viewEmail() throws Exception {
    // Tests getting multiple results and email filtering.
    List<User> users = arrangeUserData();
    List<UserTableEntry> userResults = userAdminFactory.genUserTableEntries(users);
    List<UserTableEntry> expectedResults = List.of(userResults.get(1), userResults.get(4), userResults.get(6));

    UserTableViewReq req = UserTableViewReq.builder()
        .email("@example.com") // will match all users that have emails in domain example.com
        .build();

    actAssert(req, expectedResults);
  }

  @Test
  @WithMockCustomUser(authorities = { "ROLE_OPERATOR" })
  public void viewStatus() throws Exception {
    // Tests status filtering.
    List<User> users = arrangeUserData();
    List<UserTableEntry> userResults = userAdminFactory.genUserTableEntries(users);
    List<UserTableEntry> expectedResults = List.of(userResults.get(2), userResults.get(4));

    UserTableViewReq req = UserTableViewReq.builder()
        .status(EnUserStatus.PENDING)
        .build();

    actAssert(req, expectedResults);
  }

  @Test
  @WithMockCustomUser(authorities = { "ROLE_OPERATOR" })
  public void viewLocked() throws Exception {
    // Tests locked filtering.
    List<User> users = arrangeUserData();
    List<UserTableEntry> userResults = userAdminFactory.genUserTableEntries(users);
    List<UserTableEntry> expectedResults = List.of(userResults.get(5));

    UserTableViewReq req = UserTableViewReq.builder()
        .locked(true)
        .build();

    actAssert(req, expectedResults);
  }

  @Test
  @WithMockCustomUser(authorities = { "ROLE_OPERATOR" })
  public void viewCreatedFromAt() throws Exception {
    // Tests createdFromAt filtering.
    List<User> users = arrangeUserData();
    List<UserTableEntry> userResults = userAdminFactory.genUserTableEntries(users);
    List<UserTableEntry> expectedResults = List.of(userResults.get(0), userResults.get(1), userResults.get(2), userResults.get(3));

    UserTableViewReq req = UserTableViewReq.builder()
        .createdFromAt(LocalDateTime.of(2026, 6, 9, 12, 1, 0, 0))
        .build();

    actAssert(req, expectedResults);
  }

  @Test
  @WithMockCustomUser(authorities = { "ROLE_OPERATOR" })
  public void viewCreatedToAt() throws Exception {
    // Tests createdToAt filtering.
    List<User> users = arrangeUserData();
    List<UserTableEntry> userResults = userAdminFactory.genUserTableEntries(users);
    List<UserTableEntry> expectedResults = List.of(userResults.get(4), userResults.get(5), userResults.get(6));

    UserTableViewReq req = UserTableViewReq.builder()
        .createdToAt(LocalDateTime.of(2026, 6, 9, 12, 1, 0, 0))
        .build();

    actAssert(req, expectedResults);
  }

  //

  @Test
  @WithMockCustomUser(authorities = { "ROLE_OPERATOR" })
  public void viewUnfilteredTable() throws Exception {
    List<User> users = arrangeUserData();
    List<UserTableEntry> userResults = userAdminFactory.genUserTableEntries(users);

    UserTableViewReq req = UserTableViewReq.builder().build();

    actAssert(req, userResults);
  }

  @Test
  @WithMockCustomUser(authorities = { "ROLE_OPERATOR" })
  public void viewFirstPageTable() throws Exception {
    // Tests pagination.
    List<User> users = arrangeUserData();
    List<UserTableEntry> userResults = userAdminFactory.genUserTableEntries(users);
    List<UserTableEntry> expectedResults = List.of(userResults.get(0), userResults.get(1), userResults.get(2));

    UserTableViewReq req = UserTableViewReq.builder()
        .tableMeta(TableMeta.builder().page(0).pageSize(3).build())
        .build();

    actAssert(req, expectedResults);
  }

  @Test
  @WithMockCustomUser(authorities = { "ROLE_OPERATOR" })
  public void viewSecondPageTable() throws Exception {
    // Tests pagination.
    List<User> users = arrangeUserData();
    List<UserTableEntry> userResults = userAdminFactory.genUserTableEntries(users);
    List<UserTableEntry> expectedResults = List.of(userResults.get(3), userResults.get(4), userResults.get(5));

    UserTableViewReq req = UserTableViewReq.builder()
        .tableMeta(TableMeta.builder().page(1).pageSize(3).build())
        .build();

    actAssert(req, expectedResults);
  }

  @Test
  @WithMockCustomUser(authorities = { "ROLE_OPERATOR" })
  public void viewThirdPageTable() throws Exception {
    // Tests pagination.
    List<User> users = arrangeUserData();
    List<UserTableEntry> userResults = userAdminFactory.genUserTableEntries(users);
    List<UserTableEntry> expectedResults = List.of(userResults.get(6));

    UserTableViewReq req = UserTableViewReq.builder()
        .tableMeta(TableMeta.builder().page(2).pageSize(3).build())
        .build();

    actAssert(req, expectedResults);
  }

  @Test
  @WithMockCustomUser(authorities = { "ROLE_OPERATOR" })
  public void viewReverseOrder() throws Exception {
    // Tests ordering by default column (createdAt), but in other direction.
    List<User> users = arrangeUserData();
    List<UserTableEntry> userResults = userAdminFactory.genUserTableEntries(users);
    List<UserTableEntry> expectedResults = userResults.reversed();

    UserTableViewReq req = UserTableViewReq.builder()
        .tableMeta(TableMeta.builder().sortOrder(EnSortOrder.ASC).build())
        .build();

    actAssert(req, expectedResults);
  }

  @Test
  @WithMockCustomUser(authorities = { "ROLE_OPERATOR" })
  public void viewOrderByCustomField() throws Exception {
    // Tests ordering by custom column.
    List<User> users = arrangeUserData();
    List<UserTableEntry> userResults = userAdminFactory.genUserTableEntries(users);
    List<UserTableEntry> expectedResults = userResults.stream().sorted(Comparator.comparing(UserTableEntry::email)).collect(Collectors.toList());

    UserTableViewReq req = UserTableViewReq.builder()
        .tableMeta(TableMeta.builder().sortBy("email").sortOrder(EnSortOrder.ASC).build())
        .build();

    actAssert(req, expectedResults);
  }

  //

  @Test
  @WithMockCustomUser(authorities = { "ROLE_OPERATOR" })
  public void viewMultipleFilters() throws Exception {
    // Tests multiple filters at once.
    List<User> users = arrangeUserData();
    List<UserTableEntry> userResults = userAdminFactory.genUserTableEntries(users);
    List<UserTableEntry> expectedResults = List.of(userResults.get(3), userResults.get(1));

    UserTableViewReq req = UserTableViewReq.builder()
        .status(EnUserStatus.ACTIVE)
        .createdFromAt(LocalDateTime.of(2026, 6, 9, 0, 0, 0, 0))
        .createdToAt(LocalDateTime.of(2026, 6, 9, 23, 59, 59, 999999999))
        .tableMeta(TableMeta.builder().sortBy("username").sortOrder(EnSortOrder.DESC).build())
        .build();

    actAssert(req, expectedResults);
  }

  // //////////////////////////////////////////////////////////////////////////
  // FAILURES


  @Test
  @WithMockCustomUser(authorities = { "ROLE_OPERATOR" })
  public void viewBadCreated() throws Exception {
    // Tests verification: invalid state of createdFromAt and createdToAt.
    arrangeUserData();

    UserTableViewReq req = UserTableViewReq.builder()
        .createdFromAt(LocalDateTime.of(2026, 6, 9, 23, 59, 59, 999999999))
        .createdToAt(LocalDateTime.of(2026, 6, 9, 0, 0, 0, 0))
        .build();

    // Act: Try to view table page with users.
    MvcResult mvcResult = mockMvc.perform(post("/api/admin/users")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andReturn();

    // Assert: API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.BAD_REQUEST.value());
    // Assert: Content has correct error.
    ProblemDetailBox expectedPdb = new ProblemDetailBox(
        HttpStatus.BAD_REQUEST.value(),
        "Bad request.",
        "Field createdFromAt is after createdToAt!",
        "/api/admin/users",
        "https://api.general.org/errors/general/badParams",
        Map.of()
    );
    problemDetailService.assertPd(mvcResult, expectedPdb);
  }
}
