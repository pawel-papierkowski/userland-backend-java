package org.portfolio.userland.test.helpers.asserts;

import lombok.RequiredArgsConstructor;
import org.portfolio.userland.common.dto.TableMetaResp;
import org.portfolio.userland.features.user.dto.admin.config.UserConfigTableEntry;
import org.portfolio.userland.features.user.dto.admin.config.UserConfigTableResp;
import org.portfolio.userland.features.user.dto.admin.history.UserHistoryTableEntry;
import org.portfolio.userland.features.user.dto.admin.history.UserHistoryTableResp;
import org.portfolio.userland.features.user.dto.admin.jwt.UserJwtTableEntry;
import org.portfolio.userland.features.user.dto.admin.jwt.UserJwtTableResp;
import org.portfolio.userland.features.user.dto.admin.permission.UserPermissionTableEntry;
import org.portfolio.userland.features.user.dto.admin.permission.UserPermissionTableResp;
import org.portfolio.userland.features.user.dto.admin.token.UserTokenTableEntry;
import org.portfolio.userland.features.user.dto.admin.token.UserTokenTableResp;
import org.portfolio.userland.features.user.dto.admin.user.UserTableEntry;
import org.portfolio.userland.features.user.dto.admin.user.UserTableResp;
import org.springframework.stereotype.Service;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Assert admin data.
 */
@Service
@RequiredArgsConstructor
public class UserAdminAssert {
  private static final String[] USER_TABLE_ENTRY_FIELDS_IGNORE = { "id" };
  private static final String[] USER_SUBTABLE_ENTRY_FIELDS_IGNORE = { "id" };

  /**
   * Assert user page response.
   * @param actualResp Actual user page response.
   * @param expectedResp Expected user page response.
   */
  public void assertUserPage(UserTableResp actualResp, UserTableResp expectedResp) {
    assertUserEntries(actualResp.entries(), expectedResp.entries());
    assertTableMetadata(actualResp.tableMeta(), expectedResp.tableMeta());
  }

  /**
   * Assert all user entries.
   * @param actualEntries Actual user entries.
   * @param expectedEntries Expected user entries.
   */
  private void assertUserEntries(List<UserTableEntry> actualEntries, List<UserTableEntry> expectedEntries) {
    assertThat(actualEntries.size()).as("Count of entries is wrong").isEqualTo(expectedEntries.size());
    for (int i=0; i<actualEntries.size(); i++) {
      UserTableEntry actualEntry = actualEntries.get(i);
      UserTableEntry expectedEntry = expectedEntries.get(i);
      assertUserEntry(i, actualEntry, expectedEntry);
    }
  }

  /**
   * Assert user table entry.
   * @param ix Index of entry.
   * @param actualEntry Actual user table entry.
   * @param expectedEntry Expected user table entry.
   */
  private void assertUserEntry(int ix, UserTableEntry actualEntry, UserTableEntry expectedEntry) {
    assertThat(actualEntry)
        .as("User table entry ["+ix+"] has invalid state")
        .usingRecursiveComparison()
        .ignoringFields(USER_TABLE_ENTRY_FIELDS_IGNORE)
        .isEqualTo(expectedEntry);
  }

  //

  /**
   * Assert user config page response.
   * @param actualResp Actual user config page response.
   * @param expectedResp Expected user config page response.
   */
  public void assertUserConfigPage(UserConfigTableResp actualResp, UserConfigTableResp expectedResp) {
    assertUserConfigEntries(actualResp.entries(), expectedResp.entries());
    assertTableMetadata(actualResp.tableMeta(), expectedResp.tableMeta());
  }

  /**
   * Assert all user config entries.
   * @param actualEntries Actual user config entries.
   * @param expectedEntries Expected user config entries.
   */
  private void assertUserConfigEntries(List<UserConfigTableEntry> actualEntries, List<UserConfigTableEntry> expectedEntries) {
    assertThat(actualEntries.size()).as("Count of entries is wrong").isEqualTo(expectedEntries.size());
    for (int i=0; i<actualEntries.size(); i++) {
      UserConfigTableEntry actualEntry = actualEntries.get(i);
      UserConfigTableEntry expectedEntry = expectedEntries.get(i);
      assertUserConfigEntry(i, actualEntry, expectedEntry);
    }
  }

  /**
   * Assert user config table entry.
   * @param ix Index of entry.
   * @param actualEntry Actual user config table entry.
   * @param expectedEntry Expected user config table entry.
   */
  private void assertUserConfigEntry(int ix, UserConfigTableEntry actualEntry, UserConfigTableEntry expectedEntry) {
    assertThat(actualEntry)
        .as("User config table entry ["+ix+"] has invalid state")
        .usingRecursiveComparison()
        .ignoringFields(USER_SUBTABLE_ENTRY_FIELDS_IGNORE)
        .isEqualTo(expectedEntry);
  }

  //

  /**
   * Assert user history page response.
   * @param actualResp Actual user history page response.
   * @param expectedResp Expected user history page response.
   */
  public void assertUserHistoryPage(UserHistoryTableResp actualResp, UserHistoryTableResp expectedResp) {
    assertUserHistoryEntries(actualResp.entries(), expectedResp.entries());
    assertTableMetadata(actualResp.tableMeta(), expectedResp.tableMeta());
  }

  /**
   * Assert all user history events.
   * @param actualEntries Actual user history events.
   * @param expectedEntries Expected user history events.
   */
  private void assertUserHistoryEntries(List<UserHistoryTableEntry> actualEntries, List<UserHistoryTableEntry> expectedEntries) {
    assertThat(actualEntries.size()).as("Count of events is wrong").isEqualTo(expectedEntries.size());
    for (int i=0; i<actualEntries.size(); i++) {
      UserHistoryTableEntry actualEntry = actualEntries.get(i);
      UserHistoryTableEntry expectedEntry = expectedEntries.get(i);
      assertUserHistoryEntry(i, actualEntry, expectedEntry);
    }
  }

  /**
   * Assert user history table event.
   * @param ix Index of event.
   * @param actualEntry Actual user history table event.
   * @param expectedEntry Expected user history table event.
   */
  private void assertUserHistoryEntry(int ix, UserHistoryTableEntry actualEntry, UserHistoryTableEntry expectedEntry) {
    assertThat(actualEntry)
        .as("User history table event ["+ix+"] has invalid state")
        .usingRecursiveComparison()
        .ignoringFields(USER_SUBTABLE_ENTRY_FIELDS_IGNORE)
        .isEqualTo(expectedEntry);
  }

  //

  /**
   * Assert user permission page response.
   * @param actualResp Actual user permission page response.
   * @param expectedResp Expected user permission page response.
   */
  public void assertUserPermissionPage(UserPermissionTableResp actualResp, UserPermissionTableResp expectedResp) {
    assertUserPermissionEntries(actualResp.entries(), expectedResp.entries());
    assertTableMetadata(actualResp.tableMeta(), expectedResp.tableMeta());
  }

  /**
   * Assert all user permission entries.
   * @param actualEntries Actual user permission entries.
   * @param expectedEntries Expected user permission entries.
   */
  private void assertUserPermissionEntries(List<UserPermissionTableEntry> actualEntries, List<UserPermissionTableEntry> expectedEntries) {
    assertThat(actualEntries.size()).as("Count of entries is wrong").isEqualTo(expectedEntries.size());
    for (int i=0; i<actualEntries.size(); i++) {
      UserPermissionTableEntry actualEntry = actualEntries.get(i);
      UserPermissionTableEntry expectedEntry = expectedEntries.get(i);
      assertUserPermissionEntry(i, actualEntry, expectedEntry);
    }
  }

  /**
   * Assert user permission table entry.
   * @param ix Index of entry.
   * @param actualEntry Actual user permission table entry.
   * @param expectedEntry Expected user permission table entry.
   */
  private void assertUserPermissionEntry(int ix, UserPermissionTableEntry actualEntry, UserPermissionTableEntry expectedEntry) {
    assertThat(actualEntry)
        .as("User permission table entry ["+ix+"] has invalid state")
        .usingRecursiveComparison()
        .ignoringFields(USER_SUBTABLE_ENTRY_FIELDS_IGNORE)
        .isEqualTo(expectedEntry);
  }

  //

  /**
   * Assert user token page response.
   * @param actualResp Actual user token page response.
   * @param expectedResp Expected user token page response.
   */
  public void assertUserTokenPage(UserTokenTableResp actualResp, UserTokenTableResp expectedResp) {
    assertUserTokenEntries(actualResp.entries(), expectedResp.entries());
    assertTableMetadata(actualResp.tableMeta(), expectedResp.tableMeta());
  }

  /**
   * Assert all user token entries.
   * @param actualEntries Actual user token entries.
   * @param expectedEntries Expected user token entries.
   */
  private void assertUserTokenEntries(List<UserTokenTableEntry> actualEntries, List<UserTokenTableEntry> expectedEntries) {
    assertThat(actualEntries.size()).as("Count of entries is wrong").isEqualTo(expectedEntries.size());
    for (int i=0; i<actualEntries.size(); i++) {
      UserTokenTableEntry actualEntry = actualEntries.get(i);
      UserTokenTableEntry expectedEntry = expectedEntries.get(i);
      assertUserTokenEntry(i, actualEntry, expectedEntry);
    }
  }

  /**
   * Assert user token table entry.
   * @param ix Index of entry.
   * @param actualEntry Actual user token table entry.
   * @param expectedEntry Expected user token table entry.
   */
  private void assertUserTokenEntry(int ix, UserTokenTableEntry actualEntry, UserTokenTableEntry expectedEntry) {
    assertThat(actualEntry)
        .as("User token table entry ["+ix+"] has invalid state")
        .usingRecursiveComparison()
        .ignoringFields(USER_SUBTABLE_ENTRY_FIELDS_IGNORE)
        .isEqualTo(expectedEntry);
  }

  //

  /**
   * Assert user JWT page response.
   * @param actualResp Actual user JWT page response.
   * @param expectedResp Expected user JWT page response.
   */
  public void assertUserJwtPage(UserJwtTableResp actualResp, UserJwtTableResp expectedResp) {
    assertUserJwtEntries(actualResp.entries(), expectedResp.entries());
    assertTableMetadata(actualResp.tableMeta(), expectedResp.tableMeta());
  }

  /**
   * Assert all user JWT entries.
   * @param actualEntries Actual user JWT entries.
   * @param expectedEntries Expected user JWT entries.
   */
  private void assertUserJwtEntries(List<UserJwtTableEntry> actualEntries, List<UserJwtTableEntry> expectedEntries) {
    assertThat(actualEntries.size()).as("Count of entries is wrong").isEqualTo(expectedEntries.size());
    for (int i=0; i<actualEntries.size(); i++) {
      UserJwtTableEntry actualEntry = actualEntries.get(i);
      UserJwtTableEntry expectedEntry = expectedEntries.get(i);
      assertUserJwtEntry(i, actualEntry, expectedEntry);
    }
  }

  /**
   * Assert user JWT table entry.
   * @param ix Index of entry.
   * @param actualEntry Actual user JWT table entry.
   * @param expectedEntry Expected user JWT table entry.
   */
  private void assertUserJwtEntry(int ix, UserJwtTableEntry actualEntry, UserJwtTableEntry expectedEntry) {
    assertThat(actualEntry)
        .as("User JWT table entry ["+ix+"] has invalid state")
        .usingRecursiveComparison()
        .ignoringFields(USER_SUBTABLE_ENTRY_FIELDS_IGNORE)
        .isEqualTo(expectedEntry);
  }

  // //////////////////////////////////////////////////////////////////////////

  /**
   * Assert table metadata response.
   * @param actualResp Actual table metadata response.
   * @param expectedResp Expected table metadata response.
   */
  public void assertTableMetadata(TableMetaResp actualResp, TableMetaResp expectedResp) {
    assertThat(actualResp.pageCount()).as("Page count is wrong").isEqualTo(expectedResp.pageCount());
    assertThat(actualResp.entryCount()).as("Entry count is wrong").isEqualTo(expectedResp.entryCount());
    assertThat(actualResp.page()).as("Page is wrong").isEqualTo(expectedResp.page());
    assertThat(actualResp.pageSize()).as("Page size is wrong").isEqualTo(expectedResp.pageSize());
    assertThat(actualResp.sortBy()).as("Sort by is wrong").isEqualTo(expectedResp.sortBy());
    assertThat(actualResp.sortOrder()).as("Sort order is wrong").isEqualTo(expectedResp.sortOrder());
  }
}
