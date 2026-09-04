package org.portfolio.userland.features.user.dto.admin.config;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

/**
 * DTO for editing entry of user config table.
 * @param id User config entry identifier. Can be null, in this case will create new user config entry.
 * @param userId User identifier.
 * @param name User config entry name.
 * @param value User config entry value.
 */
@Builder(toBuilder = true)
@Schema(description = "Data for user config entry change.")
public record UserConfigEditReq(
    @Schema(description = "Identifier of user config entry. Can be null, in this case will create new user config entry.", example = "2")
    Long id,

    @NotNull(message = "User identifier must be provided")
    @Schema(description = "Identifier of user.", example = "13")
    Long userId,

    @NotBlank(message = "Name of user config entry must be provided")
    @Size(max = 250, message = "Name of user config entry cannot exceed 250 characters")
    @Schema(description = "User config entry name.", example = "jwt.expires")
    String name,

    @NotBlank(message = "Value of user config entry must be provided")
    @Schema(description = "User config entry value.", example = "1440")
    String value
) {}
