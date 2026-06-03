package org.portfolio.userland.features.user.dto.admin.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import org.apache.commons.lang3.StringUtils;
import org.portfolio.userland.common.constants.ValidConst;
import org.portfolio.userland.features.user.dto.common.UserProfileData;

/**
 * Provides user and user profile data to change. Any field except <code>id</code> is optional - null simply means given
 * field is skipped.
 *
 * @param id Identificator of user.
 * @param username Username.
 * @param email Email.
 * @param locked Is user locked?
 * @param lang Language of user.
 * @param profile User profile data.
 */
@Builder(toBuilder = true)
@Schema(description = "Request for user and user profile data. All fields except id are optional.")
public record UserFullDataReq(
    @NotNull(message = "User identificator must be provided")
    @Schema(description = "Identificator of user.")
    Long id,

    @Schema(description = "Name shown on frontend.", example = "John Doe")
    String username,

    @Email(regexp = ValidConst.REG_EXPR_EMAIL, message = "Must be a valid email address")
    @Schema(description = "Email address (must be unique).", example = "john.doe@example.com")
    String email,

    @Schema(description = "Is this user locked?", example = "false")
    Boolean locked,

    @Schema(description = "Short language code.", example = "en")
    String lang,

    @Schema(description = "User profile.")
    UserProfileData profile
) {
    /**
     * Check if at least one field of user data is not empty.
     * @return True if at least one field is not empty, otherwise false.
     */
    public boolean userPresent() {
        if (StringUtils.isNotEmpty(username)) return true;
        if (StringUtils.isNotEmpty(email)) return true;
        if (locked != null) return true;
        if (StringUtils.isNotEmpty(lang)) return true;
        return false;
    }

    /**
     * Check if at least one field of user profile data is not empty.
     * @return True if at least one field is not empty, otherwise false.
     */
    public boolean userProfilePresent() {
        if (profile == null) return false;
        return profile.userProfilePresent();
    }
}
