package org.portfolio.userland.features.user.dto.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

/**
 * <p>DTO for user. It contains user data response.</p>
 * <p>To convert from <code>User</code> entity to <code>UserDataResp</code>, use mapper:</p>
 * <code><pre>
 * UserDataResp userData = userMapper.userToDataResp(user);
 * UserProfileDataResp userProfileData = userProfileMapper.profileToDataResp(user.getProfile());</pre></code>
 * @param username Username.
 * @param email User email.
 * @param lang User language as simple language code. Example: 'pl'.
 * @param profile User profile data.
 */
@Builder(toBuilder = true)
@Schema(description = "Response that returns current data of user.")
public record UserDataResp(
    @Schema(description = "Name shown on frontend.", example = "John Doe")
    String username,

    @Schema(description = "Email address (must be unique).", example = "john.doe@example.com")
    String email,

    @Schema(description = "Short language code.", example = "en")
    String lang,

    @Schema(description = "User profile.")
    UserProfileData profile
) {}
