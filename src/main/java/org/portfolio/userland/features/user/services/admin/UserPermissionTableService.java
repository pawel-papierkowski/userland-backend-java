package org.portfolio.userland.features.user.services.admin;

import lombok.RequiredArgsConstructor;
import org.portfolio.userland.common.dto.EntryMetaResp;
import org.portfolio.userland.common.dto.TableMetaReq;
import org.portfolio.userland.common.exception.ShouldNeverHappenException;
import org.portfolio.userland.common.services.table.TableHelper;
import org.portfolio.userland.features.user.dto.admin.permission.*;
import org.portfolio.userland.features.user.entities.EnUserHistoryWhat;
import org.portfolio.userland.features.user.entities.EnUserHistoryWho;
import org.portfolio.userland.features.user.entities.UserPermission;
import org.portfolio.userland.features.user.exceptions.UserCannotEditException;
import org.portfolio.userland.features.user.exceptions.UserPermissionRedundantException;
import org.portfolio.userland.system.auth.AuthHelper;
import org.portfolio.userland.system.auth.details.CustomUserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Business logic for viewing data of user permission table.
 */
@Service
@RequiredArgsConstructor
public class UserPermissionTableService extends BaseUserTableService {
  /**
   * Get page from user permission table. Request contains filtering and other (pagination, sorting) data needed to return
   * correct results.
   * @param tableReq User permission table page request.
   * @return User permission table data response.
   */
  @Transactional(readOnly = true)
  public UserPermissionTableResp getPage(UserPermissionTableReq tableReq) {
    verifyRequest(tableReq);
    TableMetaReq baseMeta = tableReq.tableMeta(); // we need sortBy with original value
    tableReq = prepareMeta(tableReq);

    Long entryCount = userPermissionRepository.countEntries(tableReq);
    List<UserPermission> userPage = userPermissionRepository.viewPage(tableReq);
    return cnvEntitiesToEntries(tableReq.userId(), userPage, baseMeta, entryCount);
  }

  /**
   * Prepare table meta separately due to sort handling.
   * <p>Note: tableMeta itself is normalized later inside {@code viewPage()}/{@code fillTableMetaResp()}, so missing
   * fields are fine here.</p>
   * @param tableReq User permission table page request.
   * @return New version of table meta request.
   */
  private UserPermissionTableReq prepareMeta(UserPermissionTableReq tableReq) {
    TableMetaReq tableMetaReq = tableReq.tableMeta();
    if (tableMetaReq != null && "name".equals(tableMetaReq.sortBy())) {
      // Name needs special sort handling.
      tableMetaReq = tableMetaReq.toBuilder().sortBy("permission.name").build();
    }
    return tableReq.toBuilder().tableMeta(tableMetaReq).build();
  }

  /**
   * Converts list of user permission entities to user permission entries in response.
   * @param userId User identifier for this entry.
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

  // //////////////////////////////////////////////////////////////////////////

  /**
   * Add or change user permission entry.
   * @param editReq User permission entry edit request.
   * @return Added or updated user permission entry.
   */
  @Transactional
  public UserPermission edit(UserPermissionEditReq editReq) {
    UserPermissionEntryInfo oldInfo = resolve(editReq.id(), editReq.userId()); // side effects are important here
    String newPerm = editReq.name() + "_" + editReq.value();

    // We need to check if same user permission already exists for this user.
    // Database enforces it, but in this way we get good, informative error instead of sad little 500.
    if (userPermissionRepository.isRedundant(editReq))
      throw new UserPermissionRedundantException(newPerm);

    LocalDateTime nowAt = clockService.getNowUTC();
    if (oldInfo == null) { // Leave trace in user history about user permission addition.
      String params = "add '" + newPerm + "'";
      addHistoryEvent(editReq.userId(), nowAt, EnUserHistoryWho.OPERATOR, EnUserHistoryWhat.EDIT_PERM, params);
    } else { // Leave trace in user history about user permission edit.
      String oldPerm = oldInfo.permissionName() + "_" + oldInfo.value();
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
   * @param entryId Identifier of entry.
   */
  @Transactional
  public void delete(Long entryId) {
    UserPermissionEntryInfo info = resolve(entryId, null);

    // Leave trace in user history about user permission deletion.
    LocalDateTime nowAt = clockService.getNowUTC();
    String params = "del '"+info.permissionName()+"_"+info.value() + "'";
    addHistoryEvent(info.userId(), nowAt, EnUserHistoryWho.OPERATOR, EnUserHistoryWhat.EDIT_PERM, params);

    // Actually delete user permission entry.
    userPermissionRepository.deleteById(entryId);
    clearJwt(info.userId());
  }

  //

  /**
   * Resolve permission entry basic info without loading full entities.
   * Also verifies access rights: logged-in user is not allowed to edit own account.
   * @param entryId Identifier of permission entry.
   * @param userId Identifier of user.
   * @return Basic info about permission entry or null if both identifiers are null.
   */
  private UserPermissionEntryInfo resolve(Long entryId, Long userId) {
    if (entryId == null && userId == null) return null;

    UserPermissionEntryInfo info = null;
    if (entryId != null) info = userPermissionRepository.findEntryInfo(entryId);

    if (userId == null) userId = info.userId();
    CustomUserDetails userDetails = AuthHelper.resolveUserDetails();
    if (userDetails == null) throw new ShouldNeverHappenException("User details should exist!");
    if (userDetails.getId().equals(userId)) // We are not allowed to edit our own account.
      throw new UserCannotEditException(userId);
    return info;
  }

  /**
   * Clear JWT entries for this user, forcing it to relog with new permissions.
   * @param userId User identifier.
   */
  private void clearJwt(Long userId) {
    userJwtRepository.deleteAllByUser(userId);
  }
}
