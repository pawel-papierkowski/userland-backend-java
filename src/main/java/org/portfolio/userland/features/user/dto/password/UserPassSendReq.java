package org.portfolio.userland.features.user.dto.password;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.portfolio.userland.common.constants.ValidConst;

/**
 * Password reset email send request.
 * @param email Email.
 */
@Schema(description = "Payload required to send email with link for password reset.")
public record UserPassSendReq(
  @NotBlank(message = "Email is required")
  @Email(regexp = ValidConst.REG_EXPR_EMAIL, message = "Must be a valid email address")
  @Schema(description = "Email address.", example = "john.doe@example.com")
  String email
) {}
