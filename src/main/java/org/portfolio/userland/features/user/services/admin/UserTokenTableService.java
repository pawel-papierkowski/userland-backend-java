package org.portfolio.userland.features.user.services.admin;

import lombok.RequiredArgsConstructor;
import org.portfolio.userland.common.dto.TableMetaReq;
import org.portfolio.userland.common.services.table.TableHelper;
import org.portfolio.userland.features.user.dto.admin.token.UserTokenTableEntry;
import org.portfolio.userland.features.user.dto.admin.token.UserTokenTableReq;
import org.portfolio.userland.features.user.dto.admin.token.UserTokenTableResp;
import org.portfolio.userland.features.user.entities.UserToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Business logic for viewing data of user token table.
 */
@Service
@RequiredArgsConstructor
public class UserTokenTableService extends BaseUserTableService {
  /**
   * Get page from user token table. Request contains filtering and other (pagination, sorting) data needed to return
   * correct results.
   * @param tableReq User token table page request.
   * @return User token table data response.
   */
  @Transactional(readOnly = true)
  public UserTokenTableResp getPage(UserTokenTableReq tableReq) {
    verifyRequest(tableReq);
    Long entryCount = userTokenRepository.countEntries(tableReq);
    List<UserToken> userPage = userTokenRepository.viewPage(tableReq);
    return cnvEntitiesToEntries(userPage, tableReq.tableMeta(), entryCount);
  }

  /**
   * Converts list of user token entities to user token entries in response.
   * @param entities List of user tokens.
   * @param tableMetaReq Metadata for table page request.
   * @param entryCount Entry count.
   * @return User token page response.
   */
  private UserTokenTableResp cnvEntitiesToEntries(List<UserToken> entities, TableMetaReq tableMetaReq, Long entryCount) {
    List<UserTokenTableEntry> entries = new ArrayList<>();
    for (UserToken entity : entities) {
      entries.add(userMapper.entityToTableEntry(entity));
    }
    return UserTokenTableResp.builder()
        .entries(entries)
        .tableMeta(TableHelper.fillTableMetaResp(tableMetaReq, entryCount))
        .build();
  }
}
