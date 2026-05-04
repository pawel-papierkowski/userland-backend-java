package org.portfolio.userland.features.user.dto.email;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import org.portfolio.userland.common.constants.ValidConst;
import org.portfolio.userland.features.user.dto.common.EnFrontendFramework;

/**
 * Email change request.
 * @param newEmail Email.
 * @param password Password.
 * @param frontend Used frontend. If null/empty, will use default.
 */
@Builder(toBuilder = true)
@Schema(description = "Payload required to send emails for email change.")
public record UserEmailChangeLinkReq(
  @NotBlank(message = "Email is required")
  @Email(regexp = ValidConst.REG_EXPR_EMAIL, message = "Must be a valid email address")
  @Schema(description = "New email address.", example = "john.doe@example.com")
  String newEmail,

  @NotBlank(message = "Password is required")
  @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
  @Pattern(
      regexp = ValidConst.REG_EXPR_PASSWORD,
      message = "Password must contain at least one uppercase letter, one lowercase letter, one number, and one special character"
  )
  @Schema(description = "Password.", example = "StrongP@ssw0rd")
  String password,

  @Schema(description = "Used frontend framework. Can be null, will default to vue.", example = "VUE")
  EnFrontendFramework frontend
) {}
