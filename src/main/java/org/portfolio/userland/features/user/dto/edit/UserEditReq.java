package org.portfolio.userland.features.user.dto.edit;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.apache.commons.lang3.StringUtils;
import org.portfolio.userland.common.constants.ValidConst;

/**
 * DTO for editing user. This one is for editing your own user account.
 * <p>Note: these fields can be null, in this case given field will be ignored. Especially useful for skipping password.</p>
 * @param username Username.
 * @param password User password.
 * @param lang User language as simple language code. Example: 'pl'.
 * @param name User name.
 * @param surname User surname.
 */
@Schema(description = "Payload required to edit user.")
public record UserEditReq(
  // USER DATA

  // basic

  @Schema(description = "Name shown on frontend. Can be nickname or similar.", example = "John Doe")
  String username,

  @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
  @Pattern(
      regexp = ValidConst.REG_EXPR_PASSWORD,
      message = "Password must contain at least one uppercase letter, one lowercase letter, one number, and one special character"
  )
  @Schema(description = "Password.", example = "StrongP@ssw0rd")
  String password,

  // options

  @Size(min = 2, max = 2, message = "Invalid language code")
  @Schema(description = "Short language code.", example = "en")
  String lang,

  // USER PROFILE DATA

  @Schema(description = "Name of user.", example = "John")
  String name,

  @Schema(description = "Surname of user.", example = "Smith")
  String surname
) {
  /**
   * Check if at least one field of user data is not empty.
   * @return True if at least one field is not empty, otherwise false.
   */
  public boolean userPresent() {
    if (StringUtils.isNotEmpty(username)) return true;
    if (StringUtils.isNotEmpty(password)) return true;
    if (StringUtils.isNotEmpty(lang)) return true;
    return false;
  }

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
