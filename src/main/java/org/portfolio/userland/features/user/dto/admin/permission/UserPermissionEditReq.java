package org.portfolio.userland.features.user.dto.admin.permission;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

/**
 * DTO for editing entry of user permission table.
 * @param userId User identificator.
 * @param id User permission entry identificator. Can be null, in this case will create new user permission entry.
 * @param name User permission entry name.
 * @param value User permission entry value.
 */
@Builder(toBuilder = true)
@Schema(description = "Data for user permission entry change.")
public record UserPermissionEditReq(
    @Schema(description = "Identificator of user permission entry. Can be null, in this case will create new user permission entry.", example = "2")
    Long id,

    @NotNull(message = "User identificator must be provided")
    @Schema(description = "Identificator of user.", example = "13")
    Long userId,

    @NotBlank(message = "Name of user permission entry must be provided")
    @Schema(description = "User permission entry name.", example = "role")
    String name,

    @NotBlank(message = "Value of user permission entry must be provided")
    @Schema(description = "User permission entry value.", example = "admin")
    String value
) {}
