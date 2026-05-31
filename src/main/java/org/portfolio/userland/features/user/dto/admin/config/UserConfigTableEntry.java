package org.portfolio.userland.features.user.dto.admin.config;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * Contains data for one user config record.
 * @param id Identificator of record.
 * @param createdAt When this record was created?
 * @param name Name of config variable. See <code>UserConfigConst</code> for available values and meaning.
 * @param value Value of config variable.
 */
@Builder(toBuilder = true)
@Schema(description = "Contains one user config table record.")
public record UserConfigTableEntry(
    @Schema(description = "Identificator of record.", example = "13")
    Long id,

    @Schema(description = "When this record was created.", example = "2026-04-21T15:27:17")
    LocalDateTime createdAt,

    @Schema(description = "Name of config variable. See UserConfigConst for available values and meaning.", example = "jwt.expire")
    String name,

    @Schema(description = "Value of config variable.", example = "1440")
    String value
) {}
