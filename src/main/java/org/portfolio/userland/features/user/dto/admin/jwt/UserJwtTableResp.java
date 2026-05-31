package org.portfolio.userland.features.user.dto.admin.jwt;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import org.portfolio.userland.common.dto.TableMetaResp;

import java.util.List;

/**
 * DTO for user JWT list result. Shows single page of entries and related metadata like count of pages.
 * @param entries Entries.
 * @param tableMeta Metadata for result.
 */
@Builder(toBuilder = true)
@Schema(description = "Response that returns page from user JWT table.")
public record UserJwtTableResp(
    @Schema(description = "Page of user JWT records.")
    List<UserJwtTableEntry> entries,

    @Schema(description = "Table metadata.")
    TableMetaResp tableMeta
) {}
