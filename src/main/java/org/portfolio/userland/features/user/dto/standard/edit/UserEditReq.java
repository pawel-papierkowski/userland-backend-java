package org.portfolio.userland.features.user.dto.standard.edit;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import org.apache.commons.lang3.StringUtils;
import org.portfolio.userland.features.user.dto.common.UserProfileData;

/**
 * DTO for editing user. This one is for editing your own user account. Contains fields both for user and user profile.
 * <p>Notes:</p>
 * <ul>
 *   <li>All fields can be null, in this case given field will be ignored.</li>
 *   <li>Certain fields cannot be changed here: password, email.</li>
 * </ul>
 * @param username Username.
 * @param lang User language as simple language code. Example: 'pl'.
 * @param profile User profile data.
 */
@Builder(toBuilder = true)
@Schema(description = "Payload required to edit user and/or user profile data. All fields can be null, in this case given field will be ignored.")
public record UserEditReq(
  // basic

  @Schema(description = "Name shown on frontend. Can be nickname or similar.", example = "John Doe")
  String username,

  // options

  @Size(min = 2, max = 2, message = "Invalid language code")
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
