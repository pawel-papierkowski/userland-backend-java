package org.portfolio.userland.features.user.dto.admin.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * Contains data for one user record.
 * @param id Identificator of record.
 * @param createdAt When this record was created?
 * @param username Username.
 * @param email User email.
 */
@Builder(toBuilder = true)
@Schema(description = "Contains one user table record.")
public record UserTableEntry(
    @Schema(description = "Identificator of record.", example = "13")
    Long id,

    @Schema(description = "When this record was created.", example = "2026-04-21T15:27:17")
    LocalDateTime createdAt,

    @Schema(description = "Name shown on frontend.", example = "John Doe")
    String username,

    @Schema(description = "Email address.", example = "john.doe@example.com")
    String email
) {}
