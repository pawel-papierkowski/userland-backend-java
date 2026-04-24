package org.portfolio.userland.system.dto.lockdown;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * Payload required to set new system lockdown state.
 * @param state New state of system lockdown.
 */
@Schema(description = "Payload required to set new system lockdown state.")
public record SystemLockdownReq(
    @NotNull(message = "State is required")
    @Schema(description = "New state of system lockdown.", example = "ON")
    EnSystemLockdownState state
) {}
