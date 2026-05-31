package org.portfolio.userland.features.user.dto.admin.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import org.portfolio.userland.common.constants.ValidConst;

import java.time.LocalDateTime;

/**
 * Contains data for one user record.
 * @param id Identificator of record.
 * @param createdAt When this record was created?
 * @param username Username.
 * @param email User email.
 */
@Builder(toBuilder = true)
@Schema(description = "Contains one table record.")
public record UserTableEntry(
    @NotBlank(message = "Id is required")
    @Schema(description = "Identificator of record.", example = "13")
    Long id,

    @NotBlank(message = "Creation date&time is required")
    @Schema(description = "When this record was created.", example = "2026-04-21T15:27:17")
    LocalDateTime createdAt,

    @NotBlank(message = "User name is required")
    @Schema(description = "Name shown on frontend.", example = "John Doe")
    String username,

    @NotBlank(message = "Email is required")
    @Email(regexp = ValidConst.REG_EXPR_EMAIL, message = "Must be a valid email address")
    @Schema(description = "Email address.", example = "john.doe@example.com")
    String email
) {}
