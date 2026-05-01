package org.portfolio.userland.system.lockdown.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

/**
 * Data about state of system lockdown.
 * @param state State of system lockdown.
 */
@Builder(toBuilder = true)
@Schema(description = "Data about state of system lockdown.")
public record SystemLockdownResp(
    @NotBlank(message = "State is required")
    @Schema(description = "State of system lockdown.", example = "OFF")
    EnSystemLockdownState state
) {}
