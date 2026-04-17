package org.portfolio.userland.features.user.dto.delete;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * User account delete confirmation request.
 * @param token Token string.
 */
@Schema(description = "Payload required to delete account.")
public record UserDeleteConfirmReq(
  @NotBlank(message = "Token string is required")
  @Size(min = 32, max = 128, message = "Token string must have 32 or more characters.")
  @Schema(description = "Token string.", example = "Pi47yVIzBdgZh3UCDpSCqmqa5UabuXu1")
  String token
) {}
