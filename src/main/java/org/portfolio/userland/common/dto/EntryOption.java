package org.portfolio.userland.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

/**
 * Option metadata for entry. It defines access rule and optional reason for it.
 * <p>For example, if we have edit option disabled, reason can contain language key that is returned by endpoint
 * and ultimately shown on frontend as tooltip explaining why this option is disabled.</p>
 * @param access Access rule.
 * @param reason Reason for state of option as language key. Can be empty string if we do not want to give reason.
 */
@Builder(toBuilder = true)
@Schema(description = "Option metadata for entry.")
public record EntryOption(
    @Schema(description = "Access rule.", example = "ENABLED")
    EnOptionAccess access,

    @Schema(description = "Reason for state of option.", example = "delete")
    String reason
) {}
