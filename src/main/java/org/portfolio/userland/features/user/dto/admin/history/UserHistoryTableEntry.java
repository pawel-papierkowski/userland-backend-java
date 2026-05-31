package org.portfolio.userland.features.user.dto.admin.history;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import org.portfolio.userland.features.user.entities.EnUserHistoryWhat;
import org.portfolio.userland.features.user.entities.EnUserHistoryWho;

import java.time.LocalDateTime;

/**
 * Contains data for one user history record.
 * @param id Identificator of record.
 * @param createdAt When this record was created?
 * @param who Who did this thing?
 * @param what What happened?
 * @param params Additional parameters.
 */
@Builder(toBuilder = true)
@Schema(description = "Contains one user history table record.")
public record UserHistoryTableEntry(
    @Schema(description = "Identificator of record.", example = "13")
    Long id,

    @Schema(description = "When this record was created.", example = "2026-04-21T15:27:17")
    LocalDateTime createdAt,

    @Schema(description = "Who did this thing.", example = "USER")
    EnUserHistoryWho who,

    @Schema(description = "What happened.", example = "EDIT")
    EnUserHistoryWhat what,

    @Schema(description = "Additional parameters.", example = "name, surname")
    String params
) {}
