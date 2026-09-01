package org.portfolio.userland.features.user.dto.standard.delete;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import org.portfolio.userland.common.annotations.ValidToken;

/**
 * User account delete confirmation request.
 * @param token Token string.
 */
@Builder(toBuilder = true)
@Schema(description = "Payload required to delete account.")
public record UserDeleteConfirmReq(
  @ValidToken
  @Schema(description = "Token string.", example = "Pi47yVIzBdgZh3UCDpSCqmqa5UabuXu1")
  String token
) {}
