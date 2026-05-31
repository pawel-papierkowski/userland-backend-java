package org.portfolio.userland.features.user.dto.admin.jwt;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * Contains data for one user JWT record.
 * @param id Identificator of record.
 * @param createdAt When this record was created?
 * @param expiresAt When this JWT will expire.
 * @param token JWT token string.
 */
@Builder(toBuilder = true)
@Schema(description = "Contains one user JWT table record.")
public record UserJwtTableEntry(
    @Schema(description = "Identificator of record.", example = "13")
    Long id,

    @Schema(description = "When this JWT was created.", example = "2026-04-21T15:27:17")
    LocalDateTime createdAt,

    @Schema(description = "When this JWT will expire.", example = "2026-04-22T15:27:17")
    LocalDateTime expiresAt,

    @Schema(description = "JWT token string.", example = "eyJhbGciOiJIUzI1NiJ9.eyJyb2xlIjoiYWRtaW4iLCJuYW1lIjoiUGF3ZcWCIFBhcGllcmtvd3NraSIsInN1YiI6InBhd2VsLnBhcGllcmtvd3NraUBnbWFpbC5jb20iLCJpYXQiOjE3ODAwNjgzNzAsImV4cCI6MTc4MDE1NDc3MH0.1SqWUyiexH9WTLt8-LpovCk8UJ74dzUyw_f-Dop4kgA")
    String token
) {}
