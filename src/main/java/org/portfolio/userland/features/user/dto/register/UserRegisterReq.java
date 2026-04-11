package org.portfolio.userland.features.user.dto.register;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.portfolio.userland.common.constants.ValidConst;

/**
 * DTO for user registration. It contains minimal subset of whole User - only data needed for registration.
 */
@Schema(description = "Payload required to register a new user.")
public record UserRegisterReq(
    @NotBlank(message = "User name is required")
    @Schema(description = "User's name shown on frontend.", example = "John Doe")
    String username,

    @NotBlank(message = "Email is required")
    @Email(regexp = ValidConst.REG_EXPR_EMAIL, message = "Must be a valid email address")
    @Schema(description = "User's email address (must be unique).", example = "john.doe@example.com")
    String email,

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
    @Schema(description = "User's password.", example = "StrongP@ssw0rd")
    String password
) {}
