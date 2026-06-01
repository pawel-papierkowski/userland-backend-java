package org.portfolio.userland.features.user.admin;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.portfolio.userland.common.dto.EnSortOrder;
import org.portfolio.userland.common.dto.TableMetaReq;
import org.portfolio.userland.common.dto.TableMetaResp;
import org.portfolio.userland.common.services.table.TableHelper;
import org.portfolio.userland.features.user.BaseUserTest;
import org.portfolio.userland.features.user.dto.admin.permission.UserPermissionTableEntry;
import org.portfolio.userland.features.user.dto.admin.permission.UserPermissionTableReq;
import org.portfolio.userland.features.user.dto.admin.permission.UserPermissionTableResp;
import org.portfolio.userland.features.user.entities.EnUserStatus;
import org.portfolio.userland.features.user.entities.Permission;
import org.portfolio.userland.features.user.entities.User;
import org.portfolio.userland.test.helpers.context.WithMockCustomUser;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Integration test for user permission table viewing.
 */
public class UserPermissionTableApiTest extends BaseUserTest {
  @BeforeEach
  public void tearDown() {
    resetDatabase();
  }

  // //////////////////////////////////////////////////////////////////////////

  /** Create user data for testing. */
  private List<User> arrangeUserData() {
    Permission permRole = permissionRepository.findByName("role").orElseThrow();
    Permission permUser = permissionRepository.findByName("user").orElseThrow();

    List<User> userList = new ArrayList<>();

    clock.setFixedTime("2026-06-10T10:00:00Z");
    User u00 = userFactory.genRandUser(EnUserStatus.ACTIVE);
    u00.setUsername("Jan Kowalski");
    u00.setEmail("jan.kowalski@test.com");
    userPermissionFactory.genPermissionEntry(u00, permRole, "admin");
    userPermissionFactory.genPermissionEntry(u00, permRole, "operator");
    userPermissionFactory.genPermissionEntry(u00, permUser, "view");
    userPermissionFactory.genPermissionEntry(u00, permUser, "edit");
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
    userPermissionFactory.genPermissionEntry(u02, permRole, "operator");
    userList.add(u02);

    return userRepository.saveAll(userList);
  }

  /**
   * Act and assert user permission results.
   * @param req Request.
   * @param expectedEntries Expected entries.
   * @param pageCount Expected page count.
   * @param entryCount Expected entry count.
   * @throws Exception When objectMapper chokes on input.
   */
  private void actAssert(UserPermissionTableReq req, List<UserPermissionTableEntry> expectedEntries, Long pageCount, Long entryCount) throws Exception {
    // Act: Try to view table page with user permissions.
    MvcResult mvcResult = mockMvc.perform(post("/api/admin/users/permissions")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andReturn();

    // Assert: API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.OK.value());

    // Assert: All results from given page returned.
    UserPermissionTableResp actualResp = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), UserPermissionTableResp.class);

    TableMetaReq tableMetaReq = TableHelper.prepareTableMeta(req.tableMeta());
    TableMetaResp expectedTableMetaResp = TableMetaResp.builder()
        .pageCount(pageCount)
        .entryCount(entryCount)
        .pageSize(tableMetaReq.pageSize())
        .page(tableMetaReq.page())
        .sortBy(tableMetaReq.sortBy())
        .sortOrder(tableMetaReq.sortOrder())
        .build();

    UserPermissionTableResp expectedResp = new UserPermissionTableResp(expectedEntries, expectedTableMetaResp);
    userAdminAssert.assertUserPermissionPage(actualResp, expectedResp);
  }

  //

  @Test
  @WithMockCustomUser(authorities = { "ROLE_OPERATOR" })
  public void viewNonexistentUser() throws Exception {
    List<User> users = arrangeUserData();
    User userToCheck = users.get(2);

    // Act: get nonexistent user.
    UserPermissionTableReq req = UserPermissionTableReq.builder().userId(userToCheck.getId()+1).build();

    actAssert(req, List.of(), 0L, 0L);
  }

  @Test
  @WithMockCustomUser(authorities = { "ROLE_OPERATOR" })
  public void viewUserWithoutPermission() throws Exception {
    List<User> users = arrangeUserData();
    User userToCheck = users.get(1);

    // Act: get user with no permission entries.
    UserPermissionTableReq req = UserPermissionTableReq.builder().userId(userToCheck.getId()).build();

    actAssert(req, List.of(), 0L, 0L);
  }

  @Test
  @WithMockCustomUser(authorities = { "ROLE_OPERATOR" })
  public void viewUserWithSinglePermission() throws Exception {
    List<User> users = arrangeUserData();
    User userToCheck = users.get(2);

    // Act: get user with single permission entry.
    UserPermissionTableReq req = UserPermissionTableReq.builder().userId(userToCheck.getId()).build();
    List<UserPermissionTableEntry> expectedResults = userAdminFactory.genUserPermissionTableEntries(userToCheck.getPermissions());

    actAssert(req, expectedResults, 1L, 1L);
  }

  @Test
  @WithMockCustomUser(authorities = { "ROLE_OPERATOR" })
  public void viewUserWithManyPermissions() throws Exception {
    List<User> users = arrangeUserData();
    User userToCheck = users.getFirst();

    // Act: get user with many permission entries.
    UserPermissionTableReq req = UserPermissionTableReq.builder()
        .userId(userToCheck.getId())
        .tableMeta(TableMetaReq.builder().sortBy("value").sortOrder(EnSortOrder.ASC).build())
        .build();
    List<UserPermissionTableEntry> permissionResults = userAdminFactory.genUserPermissionTableEntries(userToCheck.getPermissions());
    List<UserPermissionTableEntry> expectedResults = permissionResults.stream().sorted(Comparator.comparing(UserPermissionTableEntry::value)).toList();

    actAssert(req, expectedResults, 1L, 4L);
  }
}
