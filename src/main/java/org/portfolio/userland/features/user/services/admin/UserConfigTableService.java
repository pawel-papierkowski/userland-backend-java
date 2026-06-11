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
import org.portfolio.userland.features.user.exceptions.UserConfigMissingException;
import org.portfolio.userland.features.user.services.BaseUserService;
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
      UserConfigTableEntry entry = addMetaData(userMapper.entityToTableEntry(entity));
      entries.add(entry);
    }
    return UserConfigTableResp.builder()
        .entries(entries)
        .tableMeta(TableHelper.fillTableMetaResp(tableMetaReq, entryCount))
        .build();
  }

  /**
   * Add metadata to given entry.
   * @param entry Entry to amend.
   * @return Updated entry.
   */
  private UserConfigTableEntry addMetaData(UserConfigTableEntry entry) {
    Map<String, EntryOption> options = new HashMap<>();
    options.put("delete", resolveDeleteOption());
    EntryMetaResp meta = EntryMetaResp.builder()
        .options(options)
        .build();
    return entry.toBuilder()
        .meta(meta)
        .build();
  }

  /**
   * Find out state of delete option. You can delete user configuration only if you are admin.
   * @return Entry option for delete.
   */
  private EntryOption resolveDeleteOption() {
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
   * Add or change user config entry.
   * @param editReq User config entry edit request.
   * @return Added or updated user config entry.
   */
  @Transactional
  public UserConfig edit(UserConfigEditReq editReq) {
    if (editReq.id() != null && !userConfigRepository.existsById(editReq.id()))
      throw new UserConfigMissingException(editReq.id());

    return userConfigRepository.upsert(editReq);
  }

  /**
   * Deletes given user config entry.
   * @param id Identificator of entry.
   */
  @Transactional
  public void delete(Long id) {
    if (id == null || !userConfigRepository.existsById(id))
      throw new UserConfigMissingException(id);

    userConfigRepository.deleteById(id);
  }
}
