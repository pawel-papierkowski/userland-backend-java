package org.portfolio.userland.test.helpers.asserts;

import lombok.RequiredArgsConstructor;
import org.portfolio.userland.common.dto.TableMetaResp;
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
      assertUserEntry(actualEntry, expectedEntry);
    }
  }

  /**
   * Assert user table entry.
   * @param actualEntry Actual user table entry.
   * @param expectedEntry Expected user table entry.
   */
  private void assertUserEntry(UserTableEntry actualEntry, UserTableEntry expectedEntry) {
    assertThat(actualEntry)
        .as("User table entry has invalid state")
        .usingRecursiveComparison()
        .ignoringFields(USER_TABLE_ENTRY_FIELDS_IGNORE)
        .isEqualTo(expectedEntry);
  }
}
