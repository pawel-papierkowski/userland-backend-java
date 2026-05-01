package org.portfolio.userland.features.user.dto.register;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import org.portfolio.userland.features.user.dto.common.EnFrontendFramework;

/**
 * Token activation request.
 * @param token Token string.
 */
@Builder(toBuilder = true)
@Schema(description = "Payload required to register a new user.")
public record TokenActivateReq(
  @NotBlank(message = "Token string is required")
  @Size(min = 32, max = 128, message = "Token string must have 32 or more characters.")
  @Schema(description = "Token string.", example = "J4L1wZnLiw3durFYN0WDsulcpFnoKWqg")
  String token,

  @Schema(description = "Used frontend framework. Can be null, will default to vue.", example = "VUE")
  EnFrontendFramework frontend
) {}
