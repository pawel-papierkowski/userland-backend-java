package org.portfolio.userland.features.user.dto.login;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import org.portfolio.userland.common.constants.ValidConst;

/**
 * User login request.
 * @param email Email.
 * @param password Password.
 */
@Builder(toBuilder = true)
@Schema(description = "Request for user login. Uses email as login name.")
public record UserLoginReq(
  @NotBlank(message = "Email is required")
  @Email(regexp = ValidConst.REG_EXPR_EMAIL, message = "Must be a valid email address")
  @Schema(description = "Email address.", example = "john.doe@example.com")
  String email,

  @NotBlank(message = "Password is required")
  @Schema(description = "Password.", example = "StrongP@ssw0rd")
  String password
) {}
