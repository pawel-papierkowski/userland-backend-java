package org.portfolio.userland.features.user.dto.standard.password;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import org.portfolio.userland.common.annotations.ValidPassword;
import org.portfolio.userland.common.annotations.ValidToken;

/**
 * User password reset confirmation request.
 * @param token Token string.
 * @param password Password.
 */
@Builder(toBuilder = true)
@Schema(description = "Payload required to reset password.")
public record UserPassResetConfirmReq(
  @ValidToken
  @Schema(description = "Token string.", example = "Pi47yVIzBdgZh3UCDpSCqmqa5UabuXu1")
  String token,

  @ValidPassword
  @Schema(description = "Password.", example = "StrongP@ssw0rd")
  String password
) {}
