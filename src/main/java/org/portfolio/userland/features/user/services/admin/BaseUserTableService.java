package org.portfolio.userland.features.user.services.admin;

import org.portfolio.userland.common.dto.EnOptionAccess;
import org.portfolio.userland.common.dto.EntryMetaResp;
import org.portfolio.userland.common.dto.EntryOption;
import org.portfolio.userland.common.exception.BadParamsException;
import org.portfolio.userland.common.repositories.TableReq;
import org.portfolio.userland.features.user.services.BaseUserService;
import org.portfolio.userland.system.auth.AuthHelper;
import org.portfolio.userland.system.auth.details.CustomUserDetails;
import org.portfolio.userland.system.auth.perm.EnPermKind;

import java.util.HashMap;
import java.util.Map;

/**
 * For user subtable services.
 */
public class BaseUserTableService extends BaseUserService {
  /**
   * Verify request. Any error will cause exception.
   * @param tableReq User table page request.
   */
  protected void verifyRequest(TableReq tableReq) {
    if (tableReq.createdFromAt() != null && tableReq.createdToAt() != null) {
      if (tableReq.createdFromAt().isAfter(tableReq.createdToAt()))
        throw new BadParamsException("Field createdFromAt is after createdToAt!");
    }
  }

  //

  /**
   * Resolve metadata for user permission entry.
   * @param userId User identificator for this entry.
   * @return Entry metadata.
   */
  protected EntryMetaResp resolveMetadata(Long userId) {
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
}
