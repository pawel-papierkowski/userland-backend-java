package org.portfolio.userland.features.user.services.admin;

import lombok.RequiredArgsConstructor;
import org.portfolio.userland.common.dto.EntryMetaResp;
import org.portfolio.userland.common.dto.TableMetaReq;
import org.portfolio.userland.common.exception.ShouldNeverHappenException;
import org.portfolio.userland.common.services.table.TableHelper;
import org.portfolio.userland.features.user.dto.admin.config.UserConfigEditReq;
import org.portfolio.userland.features.user.dto.admin.config.UserConfigTableEntry;
import org.portfolio.userland.features.user.dto.admin.config.UserConfigTableReq;
import org.portfolio.userland.features.user.dto.admin.config.UserConfigTableResp;
import org.portfolio.userland.features.user.entities.EnUserHistoryWhat;
import org.portfolio.userland.features.user.entities.EnUserHistoryWho;
import org.portfolio.userland.features.user.entities.UserConfig;
import org.portfolio.userland.features.user.exceptions.UserCannotEditException;
import org.portfolio.userland.features.user.exceptions.UserConfigMissingException;
import org.portfolio.userland.features.user.exceptions.UserConfigRedundantException;
import org.portfolio.userland.system.auth.AuthHelper;
import org.portfolio.userland.system.auth.details.CustomUserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Business logic for viewing data of user config table.
 */
@Service
@RequiredArgsConstructor
public class UserConfigTableService extends BaseUserTableService {
  /**
   * Get page from user config table. Request contains filtering and other (pagination, sorting) data needed to return
   * correct results.
   * @param tableReq User config table page request.
   * @return User config table data response.
   */
  @Transactional(readOnly = true)
  public UserConfigTableResp getPage(UserConfigTableReq tableReq) {
    verifyRequest(tableReq);
    Long entryCount = userConfigRepository.countEntries(tableReq);
    List<UserConfig> userPage = userConfigRepository.viewPage(tableReq);
    return cnvEntitiesToEntries(tableReq.userId(), userPage, tableReq.tableMeta(), entryCount);
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
    // All entries have same metadata, as they all are for same user and there is same logged-in user (that may or may not be same user as one referred by userId).
    // Meta is not dependent on content of entity.
    EntryMetaResp meta = resolveMetadata(userId);

    List<UserConfigTableEntry> entries = new ArrayList<>();
    for (UserConfig entity : entities) {
      UserConfigTableEntry entry = toEntry(entity, meta);
      entries.add(entry);
    }
    return UserConfigTableResp.builder()
        .entries(entries)
        .tableMeta(TableHelper.fillTableMetaResp(tableMetaReq, entryCount))
        .build();
  }

  //

  /**
   * Convert entity to entry and add metadata to it.
   * @param entity User config entity.
   * @param meta Metadata for this entry.
   * @return Updated entry.
   */
  private UserConfigTableEntry toEntry(UserConfig entity, EntryMetaResp meta) {
    return userMapper.entityToTableEntry(entity).toBuilder()
        .meta(meta)
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
    UserConfig userConfig = resolve(editReq.id(), editReq.userId()); // side effects are important here
    String newConfig = editReq.name();

    // We need to check if same user config already exists for this user.
    // Database enforces it, but in this way we get good, informative error instead of sad little 500.
    if (userConfigRepository.isRedundant(editReq))
      throw new UserConfigRedundantException(newConfig);

    LocalDateTime nowAt = clockService.getNowUTC();
    if (userConfig == null) { // Leave trace in user history about user config addition.
      String params = "add '" + newConfig + "'";
      addHistoryEvent(editReq.userId(), nowAt, EnUserHistoryWho.OPERATOR, EnUserHistoryWhat.EDIT_CONFIG, params);
    } else { // Leave trace in user history about user config edit.
      String params = "set ";
      if (newConfig.equals(userConfig.getName())) params += "'" + newConfig + "'"; // changed only value, if that
      else params += "'" + userConfig.getName() + "' to '" + newConfig + "'";
      addHistoryEvent(editReq.userId(), nowAt, EnUserHistoryWho.OPERATOR, EnUserHistoryWhat.EDIT_CONFIG, params);
    }

    // Actually add/edit user config entry.
    return userConfigRepository.upsert(editReq);
  }

  /**
   * Deletes given user config entry.
   * @param entryId Identificator of entry.
   */
  @Transactional
  public void delete(Long entryId) {
    UserConfig userConfig = resolve(entryId, null); // side effects are important here

    // Leave trace in user history about user config deletion.
    LocalDateTime nowAt = clockService.getNowUTC();
    String params = "del '"+userConfig.getName() + "'";
    addHistoryEvent(userConfig.getUser(), nowAt, EnUserHistoryWho.OPERATOR, EnUserHistoryWhat.EDIT_CONFIG, params);

    // Actually delete user config entry.
    userConfigRepository.deleteById(entryId);
  }

  /**
   * Resolve configuration entry.
   * @param entryId Identificator of configuration entry. Can be null if it is new entry.
   * @param userId Identificator of user.
   * @return Configuration entry or null if it is new entry.
   */
  private UserConfig resolve(Long entryId, Long userId) {
    if (entryId == null && userId == null) return null;

    UserConfig configEntry = null;
    if (entryId != null)
      configEntry = userConfigRepository.findById(entryId).orElseThrow(()-> new UserConfigMissingException(entryId));

    if (userId == null) userId = configEntry.getUser().getId();
    CustomUserDetails userDetails = AuthHelper.resolveUserDetails();
    if (userDetails == null) throw new ShouldNeverHappenException("User details should exist!");
    if (userDetails.getId().equals(userId)) // We are not allowed to edit our own account.
      throw new UserCannotEditException(userId);
    return configEntry;
  }
}
