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
import org.portfolio.userland.features.user.entities.EnUserHistoryWhat;
import org.portfolio.userland.features.user.entities.EnUserHistoryWho;
import org.portfolio.userland.features.user.entities.UserPermission;
import org.portfolio.userland.features.user.exceptions.UserCannotEditException;
import org.portfolio.userland.features.user.exceptions.UserPermissionMissingException;
import org.portfolio.userland.features.user.exceptions.UserPermissionRedundantException;
import org.portfolio.userland.features.user.services.BaseUserService;
import org.portfolio.userland.system.auth.AuthHelper;
import org.portfolio.userland.system.auth.details.CustomUserDetails;
import org.portfolio.userland.system.auth.perm.EnPermKind;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
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
    TableMetaReq baseMeta = tableReq.tableMeta(); // we need sortBy with original value
    tableReq = prepareMeta(tableReq);

    Long entryCount = userPermissionRepository.countEntries(tableReq);
    List<UserPermission> userPage = userPermissionRepository.viewPage(tableReq);
    return cnvEntitiesToEntries(tableReq.userId(), userPage, baseMeta, entryCount);
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
   * Prepare table meta separately due to sort handling.
   * @param tableReq User permission table page request.
   * @return New version of table meta request.
   */
  private UserPermissionTableReq prepareMeta(UserPermissionTableReq tableReq) {
    TableMetaReq tableMetaReq = tableReq.tableMeta();
    if ("name".equals(tableMetaReq.sortBy())) {
      // Name needs special sort handling.
      tableMetaReq = tableMetaReq.toBuilder().sortBy("permission.name").build();
    }
    return tableReq.toBuilder().tableMeta(tableMetaReq).build();
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
   * @param userId User identificator for this entry.
   * @param entities List of user permissions.
   * @param tableMetaReq Metadata for table page request.
   * @param entryCount Entry count.
   * @return User permission page response.
   */
  private UserPermissionTableResp cnvEntitiesToEntries(Long userId, List<UserPermission> entities, TableMetaReq tableMetaReq, Long entryCount) {
    // All entries have same metadata, as they all are for same user and there is same logged-in user (that may or may not be same user as one referred by userId).
    // Meta is not dependent on content of entity.
    EntryMetaResp meta = resolveMetadata(userId);

    List<UserPermissionTableEntry> entries = new ArrayList<>();
    for (UserPermission entity : entities) {
      UserPermissionTableEntry entry = toEntry(entity, meta);
      entries.add(entry);
    }
    return UserPermissionTableResp.builder()
        .entries(entries)
        .tableMeta(TableHelper.fillTableMetaResp(tableMetaReq, entryCount))
        .build();
  }

  //

  /**
   * Convert entity to entry and add metadata to it.
   * @param entity User permission entity.
   * @param meta Metadata for this entry.
   * @return Updated entry.
   */
  private UserPermissionTableEntry toEntry(UserPermission entity, EntryMetaResp meta) {
    return userMapper.entityToTableEntry(entity).toBuilder()
        .meta(meta)
        .build();
  }

  /**
   * Resolve metadata for user permission entry.
   * @param userId User identificator for this entry.
   * @return Entry metadata.
   */
  private EntryMetaResp resolveMetadata(Long userId) {
    Map<String, EntryOption> options = new HashMap<>();
    options.put("edit", resolveOption(userId));
    options.put("delete", resolveOption(userId));
    return EntryMetaResp.builder()
        .options(options)
        .build();
  }

  /**
   * Find out state of option. You can edit/delete user permissions only if you are admin.
   * @param userId User identificator for this entry.
   * @return Entry option.
   */
  private EntryOption resolveOption(Long userId) {
    EnOptionAccess access = EnOptionAccess.ENABLED;
    String reason = null; // frontend will use default reason for tooltip

    CustomUserDetails userDetails = AuthHelper.resolveUserDetails();
    Long loggedUserId = userDetails == null ? null : userDetails.getId();
    if (userId.equals(loggedUserId)) {
      reason = "notYourself";
      access = EnOptionAccess.DISABLED;
    }
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
    UserPermission userPermission = resolve(editReq.id(), editReq.userId()); // side effects are important here
    String newPerm = editReq.name() + "_" + editReq.value();

    // We need to check if same user permission already exists for this user.
    // Database enforces it, but in this way we get good, informative error instead of sad little 500.
    if (userPermissionRepository.isRedundant(editReq))
      throw new UserPermissionRedundantException(newPerm);

    LocalDateTime nowAt = clockService.getNowUTC();
    if (userPermission == null) { // Leave trace in user history about user permission addition.
      String params = "add '" + newPerm + "'";
      addHistoryEvent(editReq.userId(), nowAt, EnUserHistoryWho.OPERATOR, EnUserHistoryWhat.EDIT_PERM, params);
    } else { // Leave trace in user history about user permission edit.
      String oldPerm = userPermission.getPermission().getName() + "_" +userPermission.getValue();
      String params = "set ";
      if (newPerm.equals(oldPerm)) params += "'" + newPerm + "'"; // no actual change
      else params += "'" + oldPerm + "' to '" + newPerm + "'";
      addHistoryEvent(editReq.userId(), nowAt, EnUserHistoryWho.OPERATOR, EnUserHistoryWhat.EDIT_PERM, params);
    }

    // Actually add/edit user permission entry.
    UserPermission permissionEntry = userPermissionRepository.upsert(editReq);
    clearJwt(editReq.userId());
    return permissionEntry;
  }

  /**
   * Deletes given user permission entry.
   * @param entryId Identificator of entry.
   */
  @Transactional
  public void delete(Long entryId) {
    UserPermission permissionEntry = resolve(entryId, null);

    // Leave trace in user history about user permission deletion.
    LocalDateTime nowAt = clockService.getNowUTC();
    String params = "del '"+permissionEntry.getPermission().getName()+"_"+permissionEntry.getValue() + "'";
    addHistoryEvent(permissionEntry.getUser(), nowAt, EnUserHistoryWho.OPERATOR, EnUserHistoryWhat.EDIT_PERM, params);

    // Actually delete user permission entry.
    userPermissionRepository.deleteById(entryId);
    clearJwt(permissionEntry.getUser().getId());
  }

  //

  /**
   * Resolve permission entry.
   * @param entryId Identificator of permission entry.
   * @param userId Identificator of user.
   * @return Permission entry.
   */
  private UserPermission resolve(Long entryId, Long userId) {
    if (entryId == null && userId == null) return null;

    UserPermission permissionEntry = null;
    if (entryId != null)
     permissionEntry = userPermissionRepository.findById(entryId).orElseThrow(()-> new UserPermissionMissingException(entryId));

    if (userId == null) userId = permissionEntry.getUser().getId();
    CustomUserDetails userDetails = AuthHelper.resolveUserDetails();
    if (userDetails == null) throw new IllegalStateException(); // Should not happen.
    if (userDetails.getId().equals(userId)) // We are not allowed to edit our own account.
      throw new UserCannotEditException(userId);
    return permissionEntry;
  }

  /**
   * Clear JWT entries for this user, forcing it to relog with new permissions.
   * @param userId User identificator.
   */
  private void clearJwt(Long userId) {
    userJwtRepository.deleteAllByUser(userId);
  }
}
