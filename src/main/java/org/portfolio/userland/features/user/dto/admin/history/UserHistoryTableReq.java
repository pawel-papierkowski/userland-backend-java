package org.portfolio.userland.features.user.dto.admin.history;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import org.portfolio.userland.common.dto.TableMetaReq;
import org.portfolio.userland.features.user.entities.EnUserHistoryWhat;
import org.portfolio.userland.features.user.entities.EnUserHistoryWho;

import java.time.LocalDateTime;

/**
 * DTO for viewing user history table. Contains filters and other settings needed for desired result.
 * All fields can be null or empty, in this case fields won't be used at all (no filtering) or defaults will be used (tableMeta).
 * @param who If present, filter by who.
 * @param what If present, filter by what.
 * @param createdFromAt If present, show user history records created at this date or later.
 * @param createdToAt If present, show user history records created at this date or earlier.
 * @param tableMeta Table metadata like pagination settings or sorting.
 */
@Builder(toBuilder = true)
@Schema(description = "Payload required to return page of results from user history table. All fields can be null/empty, in this case fields won't be used at all (no filtering) or defaults will be used (tableMeta).")
public record UserHistoryTableReq(
    @Schema(description = "Who did this thing.", example = "USER")
    EnUserHistoryWho who,

    @Schema(description = "What happened.", example = "EDIT")
    EnUserHistoryWhat what,

    @Schema(description = "If present, show user history records with creation date that is same or later.", example = "2026-04-01T12:00:00")
    LocalDateTime createdFromAt,

    @Schema(description = "If present, show user history records with creation date that is same or earlier.", example = "2026-05-24T12:00:00")
    LocalDateTime createdToAt,

    @Schema(description = "Table metadata like pagination settings or sorting.")
    TableMetaReq tableMeta
) {}
