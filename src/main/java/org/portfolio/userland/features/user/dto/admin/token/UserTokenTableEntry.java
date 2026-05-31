package org.portfolio.userland.features.user.dto.admin.token;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * Contains data for one user token record.
 * @param id Identificator of record.
 * @param createdAt When this token was created?
 * @param expiresAt When this token will expire.
 * @param token Value of token.
 * @param payload Payload of token. Only some types of tokens need payload.
 */
@Builder(toBuilder = true)
@Schema(description = "Contains one user token table record.")
public record UserTokenTableEntry(
    @Schema(description = "Identificator of record.", example = "13")
    Long id,

    @Schema(description = "When this token was created.", example = "2026-04-21T15:27:17")
    LocalDateTime createdAt,

    @Schema(description = "When this token will expire.", example = "2026-04-22T15:27:17")
    LocalDateTime expiresAt,

    @Schema(description = "Value of token.", example = "EMAIL")
    String token,

    @Schema(description = "Payload of token. Only some types of tokens need payload.", example = "new.email@example.com")
    String payload
) {}
