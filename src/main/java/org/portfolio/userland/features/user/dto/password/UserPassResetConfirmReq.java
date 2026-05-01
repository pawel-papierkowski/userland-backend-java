package org.portfolio.userland.features.user.dto.password;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import org.portfolio.userland.common.constants.ValidConst;

/**
 * User password reset confirmation request.
 * @param token Token string.
 * @param password Password.
 */
@Builder(toBuilder = true)
@Schema(description = "Payload required to reset password.")
public record UserPassResetConfirmReq(
  @NotBlank(message = "Token string is required")
  @Size(min = 32, max = 128, message = "Token string must have 32 or more characters.")
  @Schema(description = "Token string.", example = "Pi47yVIzBdgZh3UCDpSCqmqa5UabuXu1")
  String token,

  @NotBlank(message = "Password is required")
  @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
  @Pattern(
      regexp = ValidConst.REG_EXPR_PASSWORD,
      message = "Password must contain at least one uppercase letter, one lowercase letter, one number, and one special character"
  )
  @Schema(description = "Password.", example = "StrongP@ssw0rd")
  String password
) {}
