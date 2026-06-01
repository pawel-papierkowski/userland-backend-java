package org.portfolio.userland.features.user.admin;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.portfolio.userland.common.dto.EnSortOrder;
import org.portfolio.userland.common.dto.TableMetaReq;
import org.portfolio.userland.common.dto.TableMetaResp;
import org.portfolio.userland.common.services.table.TableHelper;
import org.portfolio.userland.features.user.BaseUserTest;
import org.portfolio.userland.features.user.dto.admin.token.UserTokenTableEntry;
import org.portfolio.userland.features.user.dto.admin.token.UserTokenTableReq;
import org.portfolio.userland.features.user.dto.admin.token.UserTokenTableResp;
import org.portfolio.userland.features.user.entities.EnUserStatus;
import org.portfolio.userland.features.user.entities.EnUserTokenType;
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
 * Integration test for user token table viewing.
 */
public class UserTokenTableApiTest extends BaseUserTest {
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
    clock.setFixedTime("2026-06-11T12:00:00Z");
    userTokenFactory.genTokenEntry(u00, EnUserTokenType.PASSWORD);
    clock.setFixedTime("2026-06-11T13:00:00Z");
    userTokenFactory.genTokenEntry(u00, EnUserTokenType.EMAIL, null, "new.email@aaa.pl");
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
    userTokenFactory.genTokenEntry(u02, EnUserTokenType.ACTIVATE);
    userList.add(u02);

    return userRepository.saveAll(userList);
  }

  /**
   * Act and assert user token results.
   * @param req Request.
   * @param expectedEntries Expected entries.
   * @param pageCount Expected page count.
   * @param entryCount Expected entry count.
   * @throws Exception When objectMapper chokes on input.
   */
  private void actAssert(UserTokenTableReq req, List<UserTokenTableEntry> expectedEntries, Long pageCount, Long entryCount) throws Exception {
    // Act: Try to view table page with user tokens.
    MvcResult mvcResult = mockMvc.perform(post("/api/admin/users/tokens")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andReturn();

    // Assert: API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.OK.value());

    // Assert: All results from given page returned.
    UserTokenTableResp actualResp = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), UserTokenTableResp.class);

    TableMetaReq tableMetaReq = TableHelper.prepareTableMeta(req.tableMeta());
    TableMetaResp expectedTableMetaResp = TableMetaResp.builder()
        .pageCount(pageCount)
        .entryCount(entryCount)
        .pageSize(tableMetaReq.pageSize())
        .page(tableMetaReq.page())
        .sortBy(tableMetaReq.sortBy())
        .sortOrder(tableMetaReq.sortOrder())
        .build();

    UserTokenTableResp expectedResp = new UserTokenTableResp(expectedEntries, expectedTableMetaResp);
    userAdminAssert.assertUserTokenPage(actualResp, expectedResp);
  }

  //

  @Test
  @WithMockCustomUser(authorities = { "ROLE_OPERATOR" })
  public void viewNonexistentUser() throws Exception {
    List<User> users = arrangeUserData();
    User userToCheck = users.get(2);

    // Act: get nonexistent user.
    UserTokenTableReq req = UserTokenTableReq.builder().userId(userToCheck.getId()+1).build();

    actAssert(req, List.of(), 0L, 0L);
  }

  @Test
  @WithMockCustomUser(authorities = { "ROLE_OPERATOR" })
  public void viewUserWithoutToken() throws Exception {
    List<User> users = arrangeUserData();
    User userToCheck = users.get(1);

    // Act: get user with no token entries.
    UserTokenTableReq req = UserTokenTableReq.builder().userId(userToCheck.getId()).build();

    actAssert(req, List.of(), 0L, 0L);
  }

  @Test
  @WithMockCustomUser(authorities = { "ROLE_OPERATOR" })
  public void viewUserWithSingleToken() throws Exception {
    List<User> users = arrangeUserData();
    User userToCheck = users.get(2);

    // Act: get user with single token entry.
    UserTokenTableReq req = UserTokenTableReq.builder().userId(userToCheck.getId()).build();
    List<UserTokenTableEntry> expectedResults = userAdminFactory.genUserTokenTableEntries(userToCheck.getTokens());

    actAssert(req, expectedResults, 1L, 1L);
  }

  @Test
  @WithMockCustomUser(authorities = { "ROLE_OPERATOR" })
  public void viewUserWithManyTokens() throws Exception {
    List<User> users = arrangeUserData();
    User userToCheck = users.getFirst();

    // Act: get user with many token entries.
    UserTokenTableReq req = UserTokenTableReq.builder()
        .userId(userToCheck.getId())
        .tableMeta(TableMetaReq.builder().sortBy("createdAt").sortOrder(EnSortOrder.DESC).build())
        .build();
    List<UserTokenTableEntry> tokenResults = userAdminFactory.genUserTokenTableEntries(userToCheck.getTokens());
    List<UserTokenTableEntry> expectedResults = List.of(tokenResults.get(1), tokenResults.get(0));

    actAssert(req, expectedResults, 1L, 2L);
  }
}
