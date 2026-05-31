package org.portfolio.userland.features.user.services.admin;

import lombok.RequiredArgsConstructor;
import org.portfolio.userland.common.dto.TableMetaReq;
import org.portfolio.userland.common.exception.BadParamsException;
import org.portfolio.userland.common.services.table.TableHelper;
import org.portfolio.userland.features.user.dto.admin.history.UserHistoryTableEntry;
import org.portfolio.userland.features.user.dto.admin.history.UserHistoryTableReq;
import org.portfolio.userland.features.user.dto.admin.history.UserHistoryTableResp;
import org.portfolio.userland.features.user.entities.UserHistory;
import org.portfolio.userland.features.user.services.BaseUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Business logic for viewing data of user history table.
 */
@Service
@RequiredArgsConstructor
public class UserHistoryTableService extends BaseUserService {
  /**
   * Get page from user history table. Request contains filtering and other (pagination, sorting) data needed to return
   * correct results.
   * @param tableReq User history table page request.
   * @return User history table data response.
   */
  @Transactional(readOnly = true)
  public UserHistoryTableResp getPage(UserHistoryTableReq tableReq) {
    verifyRequest(tableReq);
    tableReq = prepareRequest(tableReq);
    Long entryCount = userHistoryRepository.countEntries(tableReq);
    List<UserHistory> userPage = userHistoryRepository.viewPage(tableReq);
    return cnvEntitiesToEntries(userPage, tableReq.tableMeta(), entryCount);
  }

  /**
   * Prepare request, adding missing fields where needed.
   * @param tableReq User history table page request.
   * @return Modified user table page request.
   */
  private UserHistoryTableReq prepareRequest(UserHistoryTableReq tableReq) {
    TableMetaReq tableMetaReq = TableHelper.prepareTableMeta(tableReq.tableMeta());
    return tableReq.toBuilder()
        .tableMeta(tableMetaReq)
        .build();
  }

  /**
   * Verify request. Any error will cause exception.
   * @param tableReq User history table page request.
   */
  private void verifyRequest(UserHistoryTableReq tableReq) {
    if (tableReq.createdFromAt() != null && tableReq.createdToAt() != null) {
      if (tableReq.createdFromAt().isAfter(tableReq.createdToAt()))
        throw new BadParamsException("Field createdFromAt is after createdToAt!");
    }
  }

  /**
   * Converts list of user history entities to user history entries in response.
   * @param entities List of user historys.
   * @param tableMetaReq Metadata for table page request.
   * @param entryCount Entry count.
   * @return User history page response.
   */
  private UserHistoryTableResp cnvEntitiesToEntries(List<UserHistory> entities, TableMetaReq tableMetaReq, Long entryCount) {
    List<UserHistoryTableEntry> entries = new ArrayList<>();
    for (UserHistory entity : entities) {
      entries.add(userMapper.entityToTableEntry(entity));
    }
    return UserHistoryTableResp.builder()
        .entries(entries)
        .tableMeta(TableHelper.fillTableMetaResp(tableMetaReq, entryCount))
        .build();
  }
}
