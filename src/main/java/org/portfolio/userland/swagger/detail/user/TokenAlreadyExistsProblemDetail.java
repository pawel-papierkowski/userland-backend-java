package org.portfolio.userland.swagger.detail.user;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Shows shape of token already exists error. For Swagger documentation.
 */
@Schema(name = "TokenAlreadyExistsProblemDetail", description = "Token already exists and is still valid")
public record TokenAlreadyExistsProblemDetail(
    @Schema(example = "https://api.userland.org/errors/user/token/alreadyExists")
    String type,
    @Schema(example = "Required token already exists.")
    String title,
    @Schema(example = "409")
    int status,
    @Schema(example = "Token of type 'TOKEN_TYPE' already exists and is still valid. You cannot do this action twice in row.")
    String detail,
    @Schema(example = "/api/users/password/link")
    String instance
) {}
