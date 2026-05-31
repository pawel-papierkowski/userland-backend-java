package org.portfolio.userland.features.user.services.admin;

import lombok.RequiredArgsConstructor;
import org.portfolio.userland.common.dto.TableMetaReq;
import org.portfolio.userland.common.exception.BadParamsException;
import org.portfolio.userland.common.services.table.TableHelper;
import org.portfolio.userland.features.user.dto.admin.jwt.UserJwtTableEntry;
import org.portfolio.userland.features.user.dto.admin.jwt.UserJwtTableReq;
import org.portfolio.userland.features.user.dto.admin.jwt.UserJwtTableResp;
import org.portfolio.userland.features.user.entities.UserJwt;
import org.portfolio.userland.features.user.services.BaseUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Business logic for viewing data of user jwt table.
 */
@Service
@RequiredArgsConstructor
public class UserJwtTableService extends BaseUserService {
  /**
   * Get page from user jwt table. Request contains filtering and other (pagination, sorting) data needed to return
   * correct results.
   * @param tableReq User jwt table page request.
   * @return User jwt table data response.
   */
  @Transactional(readOnly = true)
  public UserJwtTableResp getPage(UserJwtTableReq tableReq) {
    verifyRequest(tableReq);
    tableReq = prepareRequest(tableReq);
    Long entryCount = userJwtRepository.countEntries(tableReq);
    List<UserJwt> userPage = userJwtRepository.viewPage(tableReq);
    return cnvEntitiesToEntries(userPage, tableReq.tableMeta(), entryCount);
  }

  /**
   * Prepare request, adding missing fields where needed.
   * @param tableReq User jwt table page request.
   * @return Modified user table page request.
   */
  private UserJwtTableReq prepareRequest(UserJwtTableReq tableReq) {
    TableMetaReq tableMetaReq = TableHelper.prepareTableMeta(tableReq.tableMeta());
    return tableReq.toBuilder()
        .tableMeta(tableMetaReq)
        .build();
  }

  /**
   * Verify request. Any error will cause exception.
   * @param tableReq User jwt table page request.
   */
  private void verifyRequest(UserJwtTableReq tableReq) {
    if (tableReq.createdFromAt() != null && tableReq.createdToAt() != null) {
      if (tableReq.createdFromAt().isAfter(tableReq.createdToAt()))
        throw new BadParamsException("Field createdFromAt is after createdToAt!");
    }
  }

  /**
   * Converts list of user jwt entities to user jwt entries in response.
   * @param entities List of user jwts.
   * @param tableMetaReq Metadata for table page request.
   * @param entryCount Entry count.
   * @return User jwt page response.
   */
  private UserJwtTableResp cnvEntitiesToEntries(List<UserJwt> entities, TableMetaReq tableMetaReq, Long entryCount) {
    List<UserJwtTableEntry> entries = new ArrayList<>();
    for (UserJwt entity : entities) {
      entries.add(userMapper.entityToTableEntry(entity));
    }
    return UserJwtTableResp.builder()
        .entries(entries)
        .tableMeta(TableHelper.fillTableMetaResp(tableMetaReq, entryCount))
        .build();
  }
}
