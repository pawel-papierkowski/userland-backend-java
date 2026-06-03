package org.portfolio.userland.features.user.dto.admin.config;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import org.portfolio.userland.common.dto.TableMetaReq;
import org.portfolio.userland.common.repositories.TableReq;

import java.time.LocalDateTime;

/**
 * DTO for viewing user config table. Contains filters and other settings needed for desired result.
 * All fields except userId can be null or empty, in this case fields won't be used at all (no filtering) or defaults
 * will be used (tableMeta).
 * @param userId User identificator.
 * @param createdFromAt If present, show user config records created at this date or later.
 * @param createdToAt If present, show user config records created at this date or earlier.
 * @param tableMeta Table metadata like pagination settings or sorting.
 */
@Builder(toBuilder = true)
@Schema(description = "Payload required to return page of results from user config table. All fields except userId can be null/empty, in this case fields won't be used at all (no filtering) or defaults will be used (tableMeta).")
public record UserConfigTableReq(
    @NotNull(message = "User identificator must be provided")
    @Schema(description = "Identificator of user.", example = "13")
    Long userId,

    @Schema(description = "If present, show user config records with creation date that is same or later.", example = "2026-04-01T12:00:00")
    LocalDateTime createdFromAt,

    @Schema(description = "If present, show user config records with creation date that is same or earlier.", example = "2026-05-24T12:00:00")
    LocalDateTime createdToAt,

    @Schema(description = "Table metadata like pagination settings or sorting.")
    TableMetaReq tableMeta
) implements TableReq {}
