package org.portfolio.userland.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

/**
 * Option metadata for entry.
 * @param access Access.
 * @param reason Reason for state of option as language key. Can be empty string if we do not want to give reason.
 */
@Builder(toBuilder = true)
@Schema(description = "Option metadata for entry.")
public record EntryOption(
    @Schema(description = "Access rule.", example = "ENABLED")
    EnOptionAccess access,

    @Schema(description = "Action.", example = "delete")
    String reason
) {}
