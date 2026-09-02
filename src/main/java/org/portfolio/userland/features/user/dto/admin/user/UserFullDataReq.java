package org.portfolio.userland.features.user.dto.admin.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import org.portfolio.userland.common.constants.ValidConst;
import org.portfolio.userland.features.user.dto.common.IntUserEditReq;
import org.portfolio.userland.features.user.dto.common.UserProfileData;

/**
 * Provides user and user profile data to change.
 * <p>Notes:</p>
 * <ul>
 *   <li>All fields except <code>id</code> and <code>version</code> can be null, in this case given field will be ignored.</li>
 *   <li>Field <code>version</code> is used for optimistic locking. It must contain version as returned by last read
 *   of user data. If data was modified in the meantime, request fails with 409 Conflict.</li>
 * </ul>
 *
 * @param id Identifier of user.
 * @param version Optimistic locking version of user account.
 * @param username New username.
 * @param email New email.
 * @param locked New locked.
 * @param lang New language of user.
 * @param profile New user profile data.
 */
@Builder(toBuilder = true)
@Schema(description = "Request for editing user and user profile data. All fields except id and version are optional.")
public record UserFullDataReq(
    @NotNull(message = "User identifier must be provided")
    @Schema(description = "Identifier of user.")
    Long id,

    @NotNull(message = "Version must be provided")
    @Schema(description = "Optimistic locking version of user account. Must be equal to version returned by last read of user data.", example = "3")
    Long version,

    @Size(max = 100, message = "User name cannot exceed 100 characters")
    @Schema(description = "Name shown on frontend.", example = "John Doe")
    String username,

    @Email(regexp = ValidConst.EMAIL_REGEXPR, message = "Must be a valid email address")
    @Schema(description = "Email address (must be unique).", example = "john.doe@example.com")
    String email,

    @Schema(description = "Is this user locked?", example = "false")
    Boolean locked,

    @Size(min = 2, max = 2, message = "Language code must be exactly 2 characters")
    @Schema(description = "Short language code.", example = "en")
    String lang,

    @Schema(description = "User profile.")
    UserProfileData profile
) implements IntUserEditReq {
}
