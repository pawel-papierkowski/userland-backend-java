package org.portfolio.userland.features.user.dto.standard.edit;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import org.portfolio.userland.features.user.dto.common.IntUserEditReq;
import org.portfolio.userland.features.user.dto.common.UserProfileData;

/**
 * Provides user and user profile data to change.
 * <p>Notes:</p>
 * <ul>
 *   <li>All fields except <code>version</code> can be null, in this case given field will be ignored.</li>
 *   <li>Field <code>version</code> is used for optimistic locking. It must contain version as returned by last read
 *   of user data. If data was modified in the meantime, request fails with 409 Conflict.</li>
 *   <li>Unused fields are permanently null.</li>
 * </ul>
 * @param version Optimistic locking version of user account.
 * @param username Username.
 * @param lang User language as simple language code. Example: 'pl'.
 * @param profile User profile data.
 */
@Builder(toBuilder = true)
@Schema(description = "Payload required to edit user and/or user profile data. All fields except version can be null, in this case given field will be ignored.")
public record UserEditReq(
  @NotNull(message = "Version cannot be empty")
  @Schema(description = "Optimistic locking version of user account. Must be equal to version returned by last read of user data.", example = "3")
  Long version,

  @Size(max = 100, message = "User name cannot exceed 100 characters")
  @Schema(description = "Name shown on frontend. Can be nickname or similar.", example = "John Doe")
  String username,

  @Size(min = 2, max = 2, message = "Invalid language code")
  @Schema(description = "Short language code.", example = "en")
  String lang,

  @Schema(description = "User profile.")
  UserProfileData profile
) implements IntUserEditReq {
  public Long id() {
    return null; // not editable for user profile editing
  }

  public String email() {
    return null; // not editable for user profile editing
  }

  public Boolean locked() {
    return null; // not editable for user profile editing
  }
}
