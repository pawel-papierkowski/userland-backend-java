package org.portfolio.userland.features.user.dto.admin.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

/**
 * Request for user and user profile data.
 * @param id Email of user.
 */
@Builder(toBuilder = true)
@Schema(description = "Request for user and user profile data.")
public record UserFullDataReq(
    @NotBlank(message = "Id is required")
    @Schema(description = "Identificator of user.")
    Long id
) {}
