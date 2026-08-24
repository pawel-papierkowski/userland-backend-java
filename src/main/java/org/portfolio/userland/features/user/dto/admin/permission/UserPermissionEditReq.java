package org.portfolio.userland.features.user.dto.admin.permission;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

import java.util.Locale;

/**
 * DTO for editing entry of user permission table.
 * <p>Note: <code>name</code> and <code>value</code> are normalized to lowercase in the compact constructor, so all
 * downstream code (duplicate check, persistence, history entries) always sees canonical lowercase data.</p>
 * @param userId User identificator.
 * @param id User permission entry identificator. Can be null, in this case will create new user permission entry.
 * @param name User permission entry name.
 * @param value User permission entry value.
 */
@Builder(toBuilder = true)
@Schema(description = "Data for user permission entry change.")
public record UserPermissionEditReq(
    @Schema(description = "Identificator of user permission entry. Can be null, in this case will create new user permission entry.", example = "2")
    Long id,

    @NotNull(message = "User identificator must be provided")
    @Schema(description = "Identificator of user.", example = "13")
    Long userId,

    @NotBlank(message = "Name of user permission entry must be provided")
    @Schema(description = "User permission entry name (lowercase).", example = "role")
    String name,

    @NotBlank(message = "Value of user permission entry must be provided")
    @Schema(description = "User permission entry value (lowercase).", example = "admin")
    String value
) {
  /**
   * Compact constructor. Normalizes <code>name</code> and <code>value</code> to lowercase, so mixed-case input like
   * <code>'ADMIN'</code> is stored and compared consistently with seeded permission constants.
   */
  public UserPermissionEditReq {
    if (name != null) name = name.toLowerCase(Locale.ROOT);
    if (value != null) value = value.toLowerCase(Locale.ROOT);
  }
}
