package org.portfolio.userland.features.user.dto.common;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import org.portfolio.userland.common.constants.ValidConst;

/**
 * <p>DTO for user. It contains user data response.</p>
 * <p>To convert from <code>User</code> entity to <code>UserDataResp</code>, use mapper:</p>
 * <code>UserDataResp resp = userMapper.dataFromEntity(user);</code>
 * @param username Username.
 * @param email User email.
 * @param lang User language as simple language code. Example: 'pl'.
 */
@Builder(toBuilder = true)
@Schema(description = "Response that returns current data of user.")
public record UserDataResp(
    @NotBlank(message = "User name is required")
    @Schema(description = "Name shown on frontend.", example = "John Doe")
    String username,

    @NotBlank(message = "Email is required")
    @Email(regexp = ValidConst.REG_EXPR_EMAIL, message = "Must be a valid email address")
    @Schema(description = "Email address (must be unique).", example = "john.doe@example.com")
    String email,

    @NotBlank(message = "Language is required")
    @Size(min = 2, max = 2, message = "Invalid language code")
    @Schema(description = "Short language code.", example = "en")
    String lang,

    @Schema(description = "User profile. Can be missing if profile was not affected.")
    UserProfileDataResp profile
) {}
