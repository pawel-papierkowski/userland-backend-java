package org.portfolio.userland.features.user.services.admin;

import lombok.RequiredArgsConstructor;
import org.portfolio.userland.common.dto.TableMetaReq;
import org.portfolio.userland.common.exception.BadParamsException;
import org.portfolio.userland.common.services.table.TableHelper;
import org.portfolio.userland.features.user.dto.admin.config.UserConfigTableEntry;
import org.portfolio.userland.features.user.dto.admin.config.UserConfigTableReq;
import org.portfolio.userland.features.user.dto.admin.config.UserConfigTableResp;
import org.portfolio.userland.features.user.entities.UserConfig;
import org.portfolio.userland.features.user.services.BaseUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Business logic for viewing data of user config table.
 */
@Service
@RequiredArgsConstructor
public class UserConfigTableService extends BaseUserService {
  /**
   * Get page from user config table. Request contains filtering and other (pagination, sorting) data needed to return
   * correct results.
   * @param tableReq User config table page request.
   * @return User config table data response.
   */
  @Transactional(readOnly = true)
  public UserConfigTableResp getPage(UserConfigTableReq tableReq) {
    verifyRequest(tableReq);
    tableReq = prepareRequest(tableReq);
    Long entryCount = userConfigRepository.countEntries(tableReq);
    List<UserConfig> userPage = userConfigRepository.viewPage(tableReq);
    return cnvEntitiesToEntries(userPage, tableReq.tableMeta(), entryCount);
  }

  /**
   * Prepare request, adding missing fields where needed.
   * @param tableReq User config table page request.
   * @return Modified user table page request.
   */
  private UserConfigTableReq prepareRequest(UserConfigTableReq tableReq) {
    TableMetaReq tableMetaReq = TableHelper.prepareTableMeta(tableReq.tableMeta());
    return tableReq.toBuilder()
        .tableMeta(tableMetaReq)
        .build();
  }

  /**
   * Verify request. Any error will cause exception.
   * @param tableReq User config table page request.
   */
  private void verifyRequest(UserConfigTableReq tableReq) {
    if (tableReq.createdFromAt() != null && tableReq.createdToAt() != null) {
      if (tableReq.createdFromAt().isAfter(tableReq.createdToAt()))
        throw new BadParamsException("Field createdFromAt is after createdToAt!");
    }
  }

  /**
   * Converts list of user config entities to user config entries in response.
   * @param entities List of user configs.
   * @param tableMetaReq Metadata for table page request.
   * @param entryCount Entry count.
   * @return User config page response.
   */
  private UserConfigTableResp cnvEntitiesToEntries(List<UserConfig> entities, TableMetaReq tableMetaReq, Long entryCount) {
    List<UserConfigTableEntry> entries = new ArrayList<>();
    for (UserConfig entity : entities) {
      entries.add(userMapper.entityToTableEntry(entity));
    }
    return UserConfigTableResp.builder()
        .entries(entries)
        .tableMeta(TableHelper.fillTableMetaResp(tableMetaReq, entryCount))
        .build();
  }
}
