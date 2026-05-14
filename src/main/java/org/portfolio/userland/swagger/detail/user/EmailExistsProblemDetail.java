package org.portfolio.userland.swagger.detail.user;

import io.swagger.v3.oas.annotations.media.Schema;
import org.portfolio.userland.features.user.constants.UserErrCode;

/**
 * Shows shape of user email error. For Swagger documentation.
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
    String instance,
    @Schema(example = UserErrCode.EMAIL_IN_USE)
    String errCode
) {}
