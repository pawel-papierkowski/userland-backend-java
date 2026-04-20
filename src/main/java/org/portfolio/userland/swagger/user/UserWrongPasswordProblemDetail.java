package org.portfolio.userland.swagger.user;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Shows shape of user login error. For Swagger documentation.
 */
@Schema(name = "UserWrongPasswordProblemDetail", description = "Wrong password given")
public record UserWrongPasswordProblemDetail(
    @Schema(example = "https://api.userland.org/errors/user/wrongPassword")
    String type,
    @Schema(example = "Wrong password.")
    String title,
    @Schema(example = "409")
    int status,
    @Schema(example = "Cannot log in as user with email 'john.smith@example.com' due to wrong password.")
    String detail,
    @Schema(example = "/api/users/login")
    String instance
) {}
