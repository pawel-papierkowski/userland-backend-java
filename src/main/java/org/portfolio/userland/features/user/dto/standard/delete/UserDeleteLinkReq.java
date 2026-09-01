package org.portfolio.userland.features.user.dto.standard.delete;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import org.portfolio.userland.common.annotations.ValidPassword;
import org.portfolio.userland.features.user.dto.common.EnFrontendFramework;

/**
 * Account deletion request.
 * @param password Password.
 * @param frontend Used frontend. If null/empty, will use default.
 */
@Builder(toBuilder = true)
@Schema(description = "Payload required to send email with link for account deletion.")
public record UserDeleteLinkReq(
  @ValidPassword
  @Schema(description = "Password.", example = "StrongP@ssw0rd")
  String password,

  @Schema(description = "Used frontend framework. Can be null, will default to vue.", example = "VUE")
  EnFrontendFramework frontend
) {}
