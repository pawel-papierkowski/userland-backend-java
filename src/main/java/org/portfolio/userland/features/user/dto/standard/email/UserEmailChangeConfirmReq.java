package org.portfolio.userland.features.user.dto.standard.email;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import org.portfolio.userland.common.annotations.ValidToken;

/**
 * User email change confirmation request.
 * @param token Token string.
 */
@Builder(toBuilder = true)
@Schema(description = "Payload required to change email address of user account.")
public record UserEmailChangeConfirmReq(
  @ValidToken
  @Schema(description = "Token string.", example = "Pi47yVIzBdgZh3UCDpSCqmqa5UabuXu1")
  String token
) {}
