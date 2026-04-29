package org.portfolio.userland.features.user.dto.register;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import org.portfolio.userland.common.constants.ValidConst;
import org.portfolio.userland.features.user.dto.common.EnFrontendFramework;

/**
 * <p>DTO for user registration. It contains minimal subset of whole User - only data needed for registration.</p>
 * <p>To convert from <code>UserRegisterReq</code> to <code>User</code> entity, use mapper:</p>
 * <code>User user = userMapper.registerReqToEntity(req);</code>
 * @param username Username.
 * @param email User email.
 * @param password User password.
 * @param lang User language as simple language code. Example: 'pl'.
 * @param frontend Used frontend. If null/empty, will use default. Note it is not in user data.
 */
@Builder(toBuilder = true)
@Schema(description = "Payload required to register a new user.")
public record UserRegisterReq(
    @NotBlank(message = "User name is required")
    @Schema(description = "Name shown on frontend.", example = "John Doe")
    String username,

    @NotBlank(message = "Email is required")
    @Email(regexp = ValidConst.REG_EXPR_EMAIL, message = "Must be a valid email address")
    @Schema(description = "Email address (must be unique).", example = "john.doe@example.com")
    String email,

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
    @Pattern(
        regexp = ValidConst.REG_EXPR_PASSWORD,
        message = "Password must contain at least one uppercase letter, one lowercase letter, one number, and one special character"
    )
    @Schema(description = "Password.", example = "StrongP@ssw0rd")
    String password,

    @NotBlank(message = "Language is required")
    @Size(min = 2, max = 2, message = "Invalid language code")
    @Schema(description = "Short language code.", example = "en")
    String lang,

    @Schema(description = "If true, will activate user without bothering with registration email. Ignored on PROD. Can be null, will default to false.", example = "false")
    Boolean activate,

    @Schema(description = "Used frontend framework. Can be null, will default to vue.", example = "VUE")
    EnFrontendFramework frontend
) {}
