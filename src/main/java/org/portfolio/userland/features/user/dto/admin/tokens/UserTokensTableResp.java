package org.portfolio.userland.features.user.dto.admin.tokens;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import org.portfolio.userland.common.dto.TableMetaResp;

import java.util.List;

/**
 * DTO for user token list result. Shows single page of entries and related metadata like count of pages.
 * @param entries Entries.
 * @param tableMeta Metadata for result.
 */
@Builder(toBuilder = true)
@Schema(description = "Response that returns page from user token table.")
public record UserTokensTableResp(
    @Schema(description = "Page of user token records.")
    List<UserTokensTableEntry> entries,

    @Schema(description = "Table metadata.")
    TableMetaResp tableMeta
) {}
