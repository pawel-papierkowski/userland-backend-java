package org.portfolio.userland.features.user.dto.admin.edit;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import org.portfolio.userland.features.user.dto.common.UserProfileDataResp;
import org.portfolio.userland.features.user.entities.EnUserStatus;

import java.time.LocalDateTime;

/**
 * DTO that relays most of the user and user profile entity data.
 * <p>To convert from <code>User</code> entity to <code>UserFullDataResp</code>, use mapper:</p>
 * <code><pre>
 * UserFullDataResp userData = userMapper.userToFullDataResp(user);
 * UserProfileDataResp userProfileData = userProfileMapper.profileToDataResp(profile);</pre></code>
 */
@Builder(toBuilder = true)
@Schema(description = "DTO that relays most of the user and user profile entity data.")
public record UserFullDataResp(
    @Schema(description = "Identificator.")
    Long id,

    @Schema(description = "When user was created.")
    LocalDateTime createdAt,

    @Schema(description = "When user was last modified.")
    LocalDateTime modifiedAt,

    @Schema(description = "Name shown on frontend.", example = "John Doe")
    String username,

    @Schema(description = "Email address (must be unique).", example = "john.doe@example.com")
    String email,

    @Schema(description = "Status of user.", example = "ACTIVE")
    EnUserStatus status,

    @Schema(description = "Is this user locked?", example = "false")
    Boolean locked,

    @Schema(description = "Short language code.", example = "en")
    String lang,

    @Schema(description = "User profile.")
    UserProfileDataResp profile
) {}
