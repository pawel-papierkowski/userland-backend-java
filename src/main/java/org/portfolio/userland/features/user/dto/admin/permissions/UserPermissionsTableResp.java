package org.portfolio.userland.features.user.dto.admin.permissions;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import org.portfolio.userland.common.dto.TableMetaResp;

import java.util.List;

/**
 * DTO for user permission list result. Shows single page of entries and related metadata like count of pages.
 * @param entries Entries.
 * @param tableMeta Metadata for result.
 */
@Builder(toBuilder = true)
@Schema(description = "Response that returns page from user permission table.")
public record UserPermissionsTableResp(
    @Schema(description = "Page of user permission records.")
    List<UserPermissionsTableEntry> entries,

    @Schema(description = "Table metadata.")
    TableMetaResp tableMeta
) {}
