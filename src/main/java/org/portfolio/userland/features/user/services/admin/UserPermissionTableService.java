package org.portfolio.userland.features.user.services.admin;

import lombok.RequiredArgsConstructor;
import org.portfolio.userland.common.dto.EnOptionAccess;
import org.portfolio.userland.common.dto.EntryMetaResp;
import org.portfolio.userland.common.dto.EntryOption;
import org.portfolio.userland.common.dto.TableMetaReq;
import org.portfolio.userland.common.exception.BadParamsException;
import org.portfolio.userland.common.services.table.TableHelper;
import org.portfolio.userland.features.user.dto.admin.permission.UserPermissionEditReq;
import org.portfolio.userland.features.user.dto.admin.permission.UserPermissionTableEntry;
import org.portfolio.userland.features.user.dto.admin.permission.UserPermissionTableReq;
import org.portfolio.userland.features.user.dto.admin.permission.UserPermissionTableResp;
import org.portfolio.userland.features.user.entities.UserPermission;
import org.portfolio.userland.features.user.exceptions.UserPermissionMissingException;
import org.portfolio.userland.features.user.exceptions.UserPermissionRedundantException;
import org.portfolio.userland.features.user.services.BaseUserService;
import org.portfolio.userland.system.auth.perm.EnPermKind;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
      UserPermissionTableEntry entry = addMetaData(userMapper.entityToTableEntry(entity));
      entries.add(entry);
    }
    return UserPermissionTableResp.builder()
        .entries(entries)
        .tableMeta(TableHelper.fillTableMetaResp(tableMetaReq, entryCount))
        .build();
  }

  //

  /**
   * Add metadata to given entry.
   * @param entry Entry to amend.
   * @return Updated entry.
   */
  private UserPermissionTableEntry addMetaData(UserPermissionTableEntry entry) {
    Map<String, EntryOption> options = new HashMap<>();
    options.put("edit", resolveOption());
    options.put("delete", resolveOption());
    EntryMetaResp meta = EntryMetaResp.builder()
        .options(options)
        .build();
    return entry.toBuilder()
        .meta(meta)
        .build();
  }

  /**
   * Find out state of option. You can edit/delete user permissions only if you are admin.
   * @return Entry option.
   */
  private EntryOption resolveOption() {
    EnOptionAccess access = EnOptionAccess.ENABLED;
    String reason = null; // frontend will use default reason for tooltip

    if (!permissionService.has(EnPermKind.ADMIN_ONLY)) {
      reason = "adminOnly";
      access = EnOptionAccess.DISABLED;
    }
    return EntryOption.builder()
        .access(access)
        .reason(reason)
        .build();
  }

  // //////////////////////////////////////////////////////////////////////////

  /**
   * Add or change user permission entry.
   * @param editReq User permission entry edit request.
   * @return Added or updated user permission entry.
   */
  @Transactional
  public UserPermission edit(UserPermissionEditReq editReq) {
    if (editReq.id() != null && !userPermissionRepository.existsById(editReq.id()))
      throw new UserPermissionMissingException(editReq.id());

    // We need to check if same user permission already exists for this user.
    // Database enforces it, but in this way we get good, informative error instead of sad little 500.
    if (userPermissionRepository.isRedundant(editReq))
      throw new UserPermissionRedundantException(editReq.name()+"_"+editReq.value());

    UserPermission userPermission = userPermissionRepository.upsert(editReq);
    clearJwt(editReq.userId());
    return userPermission;
  }

  /**
   * Deletes given user permission entry.
   * @param id Identificator of entry.
   */
  @Transactional
  public void delete(Long id) {
    UserPermission userPermission = userPermissionRepository.findById(id).orElse(null);
    if (userPermission == null) throw new UserPermissionMissingException(id);

    userPermissionRepository.deleteById(id);
    clearJwt(userPermission.getUser().getId());
  }

  //

  /**
   * Clear JWT entries for this user, forcing it to relog with new permissions.
   * @param userId User identificator.
   */
  private void clearJwt(Long userId) {
    userJwtRepository.deleteAllByUser(userId);
  }
}
