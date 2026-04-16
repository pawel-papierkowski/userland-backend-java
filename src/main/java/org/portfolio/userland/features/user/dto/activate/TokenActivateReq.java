package org.portfolio.userland.features.user.dto.activate;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Token activation request.
 * @param token Token string.
 */
@Schema(description = "Payload required to register a new user.")
public record TokenActivateReq(
  @NotBlank(message = "Token string is required")
  @Size(min = 32, max = 128, message = "Token string must have 32 or more characters.")
  @Schema(description = "Token string.", example = "J4L1wZnLiw3durFYN0WDsulcpFnoKWqg")
  String token
) {}
