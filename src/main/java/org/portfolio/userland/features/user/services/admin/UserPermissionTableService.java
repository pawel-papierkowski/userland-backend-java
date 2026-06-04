package org.portfolio.userland.features.user.services.admin;

import lombok.RequiredArgsConstructor;
import org.portfolio.userland.common.dto.TableMetaReq;
import org.portfolio.userland.common.exception.BadParamsException;
import org.portfolio.userland.common.services.table.TableHelper;
import org.portfolio.userland.features.user.dto.admin.permission.UserPermissionEditReq;
import org.portfolio.userland.features.user.dto.admin.permission.UserPermissionTableEntry;
import org.portfolio.userland.features.user.dto.admin.permission.UserPermissionTableReq;
import org.portfolio.userland.features.user.dto.admin.permission.UserPermissionTableResp;
import org.portfolio.userland.features.user.entities.UserPermission;
import org.portfolio.userland.features.user.exceptions.UserPermissionMissingException;
import org.portfolio.userland.features.user.services.BaseUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Business logic for viewing data of user permission table.
 */
@Service
@RequiredArgsConstructor
public class UserPermissionTableService extends BaseUserService {
  /**
   * Get page from user permission table. Request contains filtering and other (pagination, sorting) data needed to return
   * correct results.
   * @param tableReq User permission table page request.
   * @return User permission table data response.
   */
  @Transactional(readOnly = true)
  public UserPermissionTableResp getPage(UserPermissionTableReq tableReq) {
    verifyRequest(tableReq);
    tableReq = prepareRequest(tableReq);
    Long entryCount = userPermissionRepository.countEntries(tableReq);
    List<UserPermission> userPage = userPermissionRepository.viewPage(tableReq);
    return cnvEntitiesToEntries(userPage, tableReq.tableMeta(), entryCount);
  }

  /**
   * Prepare request, adding missing fields where needed.
   * @param tableReq User permission table page request.
   * @return Modified user table page request.
   */
  private UserPermissionTableReq prepareRequest(UserPermissionTableReq tableReq) {
    TableMetaReq tableMetaReq = TableHelper.prepareTableMeta(tableReq.tableMeta());
    return tableReq.toBuilder()
        .tableMeta(tableMetaReq)
        .build();
  }

  /**
   * Verify request. Any error will cause exception.
   * @param tableReq User permission table page request.
   */
  private void verifyRequest(UserPermissionTableReq tableReq) {
    if (tableReq.createdFromAt() != null && tableReq.createdToAt() != null) {
      if (tableReq.createdFromAt().isAfter(tableReq.createdToAt()))
        throw new BadParamsException("Field createdFromAt is after createdToAt!");
    }
  }

  /**
   * Converts list of user permission entities to user permission entries in response.
   * @param entities List of user permissions.
   * @param tableMetaReq Metadata for table page request.
   * @param entryCount Entry count.
   * @return User permission page response.
   */
  private UserPermissionTableResp cnvEntitiesToEntries(List<UserPermission> entities, TableMetaReq tableMetaReq, Long entryCount) {
    List<UserPermissionTableEntry> entries = new ArrayList<>();
    for (UserPermission entity : entities) {
      entries.add(userMapper.entityToTableEntry(entity));
    }
    return UserPermissionTableResp.builder()
        .entries(entries)
        .tableMeta(TableHelper.fillTableMetaResp(tableMetaReq, entryCount))
        .build();
  }

  // //////////////////////////////////////////////////////////////////////////

  /**
   * Add or change user permission entry.
   * @param editReq User permission entry edit request.
   * @return Added or updated user permission entry.
   */
  public UserPermission edit(UserPermissionEditReq editReq) {
    if (editReq.id() != null && !userPermissionRepository.existsById(editReq.id()))
      throw new UserPermissionMissingException(editReq.id());

    return userPermissionRepository.upsert(editReq);
  }

  /**
   * Deletes given user permission entry.
   * @param id Identificator of entry.
   */
  public void delete(Long id) {
    if (id == null || !userPermissionRepository.existsById(id))
      throw new UserPermissionMissingException(id);

    userPermissionRepository.deleteById(id);
  }
}
