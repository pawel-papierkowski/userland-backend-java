package org.portfolio.userland.features.user.admin;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.portfolio.userland.common.dto.EnSortOrder;
import org.portfolio.userland.common.dto.TableMetaReq;
import org.portfolio.userland.common.dto.TableMetaResp;
import org.portfolio.userland.common.services.table.TableHelper;
import org.portfolio.userland.features.user.BaseUserTest;
import org.portfolio.userland.features.user.dto.admin.jwt.UserJwtTableEntry;
import org.portfolio.userland.features.user.dto.admin.jwt.UserJwtTableReq;
import org.portfolio.userland.features.user.dto.admin.jwt.UserJwtTableResp;
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
 * Integration test for user JWT table viewing.
 */
public class UserJwtTableApiTest extends BaseUserTest {
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
    clock.setFixedTime("2026-06-11T13:00:00Z");
    userJwtFactory.genJwtEntry(u00, "fake.jwt.string");
    clock.setFixedTime("2026-06-11T14:00:00Z");
    userJwtFactory.genJwtEntry(u00, "other.jwt.string");
    clock.setFixedTime("2026-06-11T15:00:00Z");
    userJwtFactory.genJwtEntry(u00, "apparently.jwt.string");
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
    userJwtFactory.genJwtEntry(u02, "different.jwt.string");
    userList.add(u02);

    return userRepository.saveAll(userList);
  }

  /**
   * Act and assert user JWT results.
   * @param req Request.
   * @param expectedEntries Expected entries.
   * @param pageCount Expected page count.
   * @param entryCount Expected entry count.
   * @throws Exception When objectMapper chokes on input.
   */
  private void actAssert(UserJwtTableReq req, List<UserJwtTableEntry> expectedEntries, Long pageCount, Long entryCount) throws Exception {
    // Act: Try to view table page with user JWTs.
    MvcResult mvcResult = mockMvc.perform(post("/api/admin/user/jwt")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(req)))
        .andReturn();

    // Assert: API Response.
    assertThat(mvcResult.getResponse().getStatus()).as("HTTP status is wrong").isEqualTo(HttpStatus.OK.value());

    // Assert: All results from given page returned.
    UserJwtTableResp actualResp = objectMapper.readValue(mvcResult.getResponse().getContentAsString(), UserJwtTableResp.class);

    TableMetaReq tableMetaReq = TableHelper.prepareTableMeta(req.tableMeta());
    TableMetaResp expectedTableMetaResp = TableMetaResp.builder()
        .pageCount(pageCount)
        .entryCount(entryCount)
        .pageSize(tableMetaReq.pageSize())
        .page(tableMetaReq.page())
        .sortBy(tableMetaReq.sortBy())
        .sortOrder(tableMetaReq.sortOrder())
        .build();

    UserJwtTableResp expectedResp = new UserJwtTableResp(expectedEntries, expectedTableMetaResp);
    userAdminAssert.assertUserJwtPage(actualResp, expectedResp);
  }

  //

  @Test
  @WithMockCustomUser(authorities = { "ROLE_OPERATOR" })
  public void viewNonexistentUser() throws Exception {
    List<User> users = arrangeUserData();
    User userToCheck = users.get(2);

    // Act: get nonexistent user.
    UserJwtTableReq req = UserJwtTableReq.builder().userId(userToCheck.getId()+1).build();

    actAssert(req, List.of(), 0L, 0L);
  }

  @Test
  @WithMockCustomUser(authorities = { "ROLE_OPERATOR" })
  public void viewUserWithoutJwt() throws Exception {
    List<User> users = arrangeUserData();
    User userToCheck = users.get(1);

    // Act: get user with no JWT entries.
    UserJwtTableReq req = UserJwtTableReq.builder().userId(userToCheck.getId()).build();

    actAssert(req, List.of(), 0L, 0L);
  }

  @Test
  @WithMockCustomUser(authorities = { "ROLE_OPERATOR" })
  public void viewUserWithSingleJwt() throws Exception {
    List<User> users = arrangeUserData();
    User userToCheck = users.get(2);

    // Act: get user with single JWT entry.
    UserJwtTableReq req = UserJwtTableReq.builder().userId(userToCheck.getId()).build();
    List<UserJwtTableEntry> expectedResults = userAdminFactory.genUserJwtTableEntries(userToCheck.getJwts());

    actAssert(req, expectedResults, 1L, 1L);
  }

  @Test
  @WithMockCustomUser(authorities = { "ROLE_OPERATOR" })
  public void viewUserWithManyJwts() throws Exception {
    List<User> users = arrangeUserData();
    User userToCheck = users.getFirst();

    // Act: get user with many JWT entries.
    UserJwtTableReq req = UserJwtTableReq.builder()
        .userId(userToCheck.getId())
        .tableMeta(TableMetaReq.builder().sortBy("createdAt").sortOrder(EnSortOrder.DESC).build())
        .build();
    List<UserJwtTableEntry> jwtResults = userAdminFactory.genUserJwtTableEntries(userToCheck.getJwts());
    List<UserJwtTableEntry> expectedResults = List.of(jwtResults.get(0), jwtResults.get(1), jwtResults.get(2));

    actAssert(req, expectedResults, 1L, 3L);
  }
}
