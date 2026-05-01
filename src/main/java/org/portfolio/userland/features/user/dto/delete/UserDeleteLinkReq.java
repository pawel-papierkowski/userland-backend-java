package org.portfolio.userland.features.user.dto.delete;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import org.portfolio.userland.common.constants.ValidConst;
import org.portfolio.userland.features.user.dto.common.EnFrontendFramework;

/**
 * Email with link for account deletion request.
 * @param email Email.
 * @param frontend Used frontend. If null/empty, will use default.
 */
@Builder(toBuilder = true)
@Schema(description = "Payload required to send email with link for account deletion.")
public record UserDeleteLinkReq(
  @NotBlank(message = "Email is required")
  @Email(regexp = ValidConst.REG_EXPR_EMAIL, message = "Must be a valid email address")
  @Schema(description = "Email address.", example = "john.doe@example.com")
  String email,

  @Schema(description = "Used frontend framework. Can be null, will default to vue.", example = "VUE")
  EnFrontendFramework frontend
) {}
