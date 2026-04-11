package org.portfolio.userland.swagger.user;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Shows shape of email error. For Swagger documentation.
 */
@Schema(name = "EmailExistsProblemDetail", description = "Email already exists")
public record EmailExistsProblemDetail(
    @Schema(example = "https://api.userland.org/errors/user/email/alreadyExists")
    String type,
    @Schema(example = "User with given email already exists.")
    String title,
    @Schema(example = "409")
    int status,
    @Schema(example = "Email 'test@test.com' already exists.")
    String detail,
    @Schema(example = "/api/users/register")
    String instance
) {}
