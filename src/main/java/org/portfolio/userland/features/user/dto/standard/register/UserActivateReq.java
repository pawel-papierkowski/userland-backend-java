package org.portfolio.userland.features.user.dto.standard.register;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import org.portfolio.userland.common.annotations.ValidToken;
import org.portfolio.userland.features.user.dto.common.EnFrontendFramework;

/**
 * User activation request.
 * @param token Token string.
 * @param frontend Used frontend framework.
 */
@Builder(toBuilder = true)
@Schema(description = "Payload required to activate user.")
public record UserActivateReq(
  @ValidToken
  @Schema(description = "Token string.", example = "J4L1wZnLiw3durFYN0WDsulcpFnoKWqg")
  String token,

  @Schema(description = "Used frontend framework. Can be null, will default to vue.", example = "VUE")
  EnFrontendFramework frontend
) {}
