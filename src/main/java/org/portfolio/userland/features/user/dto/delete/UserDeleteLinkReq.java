package org.portfolio.userland.features.user.dto.delete;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.portfolio.userland.common.constants.ValidConst;

/**
 * Email with link for account deletion request.
 * @param email Email.
 */
@Schema(description = "Payload required to send email with link for account deletion.")
public record UserDeleteLinkReq(
  @NotBlank(message = "Email is required")
  @Email(regexp = ValidConst.REG_EXPR_EMAIL, message = "Must be a valid email address")
  @Schema(description = "Email address.", example = "john.doe@example.com")
  String email
) {}
