package org.portfolio.userland.swagger.detail.user;

import io.swagger.v3.oas.annotations.media.Schema;
import org.portfolio.userland.features.user.constants.UserErrCode;

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
    String instance,
    @Schema(example = UserErrCode.WRONG_PASSWORD)
    String errCode
) {}
