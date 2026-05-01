package org.portfolio.userland.features.user.dto.common;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

/**
 * <p>DTO for user profile. It contains user profile data response.</p>
 * <p>To convert from <code>UserProfile</code> entity to <code>UserProfileDataResp</code>, use mapper:</p>
 * <code>UserProfileDataResp resp = userProfileMapper.dataFromEntity(userProfile);</code>
 * @param name Name.
 * @param surname Surname.
 */
@Builder(toBuilder = true)
@Schema(description = "Response that returns current data of user profile.")
public record UserProfileDataResp(
    @NotBlank(message = "Name is required")
    @Schema(description = "Name of user.", example = "John")
    String name,

    @NotBlank(message = "Surname is required")
    @Schema(description = "Surname of user.", example = "Doe")
    String surname
) {}
