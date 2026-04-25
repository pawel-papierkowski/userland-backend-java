package org.portfolio.userland.swagger.detail.user;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Shows shape of non-existent user error. For Swagger documentation.
 */
@Schema(name = "UserDoesNotExistProblemDetail", description = "User does not exist")
public record UserDoesNotExistProblemDetail(
    @Schema(example = "https://api.userland.org/errors/user/doesNotExist")
    String type,
    @Schema(example = "User does not exist.")
    String title,
    @Schema(example = "404")
    int status,
    @Schema(example = "User with email 'john.smith@example.com' does not exist.")
    String detail,
    @Schema(example = "/api/users/password/link")
    String instance
) {}
