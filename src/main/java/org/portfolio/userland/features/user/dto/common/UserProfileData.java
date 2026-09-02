package org.portfolio.userland.features.user.dto.common;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import org.apache.commons.lang3.StringUtils;

/**
 * <p>DTO for user profile. It contains user profile data.</p>
 * <p>To convert from <code>UserProfile</code> entity to <code>UserProfileData</code>, use mapper:</p>
 * <code>UserProfileData resp = userMapper.profileToData(userProfile);</code>
 * @param name Name.
 * @param surname Surname.
 */
@Builder(toBuilder = true)
@Schema(description = "Contains data of user profile.")
public record UserProfileData(
    @Size(max = 100, message = "Name cannot exceed 100 characters")
    @Schema(description = "Name of user.", example = "John")
    String name,

    @Size(max = 100, message = "Surname cannot exceed 100 characters")
    @Schema(description = "Surname of user.", example = "Smith")
    String surname
) {
    /**
     * Check if at least one field of user profile data is not empty.
     * @return True if at least one field is not empty, otherwise false.
     */
    public boolean userProfilePresent() {
        if (StringUtils.isNotEmpty(name)) return true;
        if (StringUtils.isNotEmpty(surname)) return true;
        return false;
    }
}
