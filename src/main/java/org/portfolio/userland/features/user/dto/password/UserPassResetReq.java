package org.portfolio.userland.features.user.dto.password;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * User password reset request.
 * @param token Token string.
 */
@Schema(description = "Payload required to reset password.")
public record UserPassResetReq(
  @NotBlank(message = "Token string is required")
  @Size(min = 32, max = 128, message = "Token string must have 32 or more characters.")
  @Schema(description = "Token string.", example = "Pi47yVIzBdgZh3UCDpSCqmqa5UabuXu1")
  String token,

  @NotBlank(message = "Password is required")
  @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
  @Schema(description = "Password.", example = "StrongP@ssw0rd")
  String password
) {}
