package org.portfolio.userland.swagger.user;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Shows shape of token error. For Swagger documentation.
 */
@Schema(name = "TokenExpiredProblemDetail", description = "Token is expired")
public record TokenExpiredProblemDetail(
    @Schema(example = "https://api.userland.org/errors/user/token/expired")
    String type,
    @Schema(example = "User token is expired.")
    String title,
    @Schema(example = "409")
    int status,
    @Schema(example = "Token 'EXPIRED_TOKEN' already expired.")
    String detail,
    @Schema(example = "/api/users/activate")
    String instance
) {}
