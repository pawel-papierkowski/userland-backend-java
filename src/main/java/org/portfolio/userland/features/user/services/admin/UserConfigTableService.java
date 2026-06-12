package org.portfolio.userland.features.user.services.admin;

import lombok.RequiredArgsConstructor;
import org.portfolio.userland.common.dto.EnOptionAccess;
import org.portfolio.userland.common.dto.EntryMetaResp;
import org.portfolio.userland.common.dto.EntryOption;
import org.portfolio.userland.common.dto.TableMetaReq;
import org.portfolio.userland.common.exception.BadParamsException;
import org.portfolio.userland.common.services.table.TableHelper;
import org.portfolio.userland.features.user.dto.admin.config.UserConfigEditReq;
import org.portfolio.userland.features.user.dto.admin.config.UserConfigTableEntry;
import org.portfolio.userland.features.user.dto.admin.config.UserConfigTableReq;
import org.portfolio.userland.features.user.dto.admin.config.UserConfigTableResp;
import org.portfolio.userland.features.user.entities.UserConfig;
import org.portfolio.userland.features.user.exceptions.UserCannotEditException;
import org.portfolio.userland.features.user.exceptions.UserConfigMissingException;
import org.portfolio.userland.features.user.exceptions.UserConfigRedundantException;
import org.portfolio.userland.features.user.services.BaseUserService;
import org.portfolio.userland.system.auth.AuthHelper;
import org.portfolio.userland.system.auth.details.CustomUserDetails;
import org.portfolio.userland.system.auth.perm.EnPermKind;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    return cnvEntitiesToEntries(tableReq.userId(), userPage, tableReq.tableMeta(), entryCount);
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
   * @param userId User identificator for this entry.
   * @param entities List of user configs.
   * @param tableMetaReq Metadata for table page request.
   * @param entryCount Entry count.
   * @return User config page response.
   */
  private UserConfigTableResp cnvEntitiesToEntries(Long userId, List<UserConfig> entities, TableMetaReq tableMetaReq, Long entryCount) {
    List<UserConfigTableEntry> entries = new ArrayList<>();
    for (UserConfig entity : entities) {
      UserConfigTableEntry entry = addMetaData(userId, userMapper.entityToTableEntry(entity));
      entries.add(entry);
    }
    return UserConfigTableResp.builder()
        .entries(entries)
        .tableMeta(TableHelper.fillTableMetaResp(tableMetaReq, entryCount))
        .build();
  }

  //

  /**
   * Add metadata to given entry.
   * @param userId User identificator for this entry.
   * @param entry Entry to amend.
   * @return Updated entry.
   */
  private UserConfigTableEntry addMetaData(Long userId, UserConfigTableEntry entry) {
    Map<String, EntryOption> options = new HashMap<>();
    options.put("edit", resolveOption(userId));
    options.put("delete", resolveOption(userId));
    EntryMetaResp meta = EntryMetaResp.builder()
        .options(options)
        .build();
    return entry.toBuilder()
        .meta(meta)
        .build();
  }

  /**
   * Find out state of option. You can edit/delete user configuration only if you are admin.
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
   * Add or change user config entry.
   * @param editReq User config entry edit request.
   * @return Added or updated user config entry.
   */
  @Transactional
  public UserConfig edit(UserConfigEditReq editReq) {
    resolve(editReq.id(), editReq.userId()); // side effects are important here

    // We need to check if same user config already exists for this user.
    // Database enforces it, but in this way we get good, informative error instead of sad little 500.
    if (userConfigRepository.isRedundant(editReq))
      throw new UserConfigRedundantException(editReq.name());

    return userConfigRepository.upsert(editReq);
  }

  /**
   * Deletes given user config entry.
   * @param entryId Identificator of entry.
   */
  @Transactional
  public void delete(Long entryId) {
    resolve(entryId, null); // side effects are important here
    userConfigRepository.deleteById(entryId);
  }

  /**
   * Resolve configuration entry.
   * @param entryId Identificator of configuration entry.
   * @param userId Identificator of user.
   * @return Configuration entry.
   */
  private UserConfig resolve(Long entryId, Long userId) {
    if (entryId == null && userId == null) return null;

    UserConfig configEntry = null;
    if (entryId != null)
      configEntry = userConfigRepository.findById(entryId).orElseThrow(()-> new UserConfigMissingException(entryId));

    if (userId == null) userId = configEntry.getUser().getId();
    CustomUserDetails userDetails = AuthHelper.resolveUserDetails();
    if (userDetails == null) throw new IllegalStateException(); // Should not happen.
    if (userDetails.getId().equals(userId)) // We are not allowed to edit our own account.
      throw new UserCannotEditException(userId);
    return configEntry;
  }
}
