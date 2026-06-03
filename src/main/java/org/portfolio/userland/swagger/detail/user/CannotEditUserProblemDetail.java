package org.portfolio.userland.swagger.detail.user;

import io.swagger.v3.oas.annotations.media.Schema;
import org.portfolio.userland.features.user.constants.UserErrCode;

/**
 * Shows shape of non-editable user error. For Swagger documentation.
 */
@Schema(name = "CannotEditUserProblemDetail", description = "Cannot edit this user")
public record CannotEditUserProblemDetail(
    @Schema(example = "https://api.userland.org/errors/user/cannotEdit")
    String type,
    @Schema(example = "Not allowed to edit this user.")
    String title,
    @Schema(example = "404")
    int status,
    @Schema(example = "User with id '42' cannot be edited.")
    String detail,
    @Schema(example = "/api/users/password/link")
    String instance,
    @Schema(example = UserErrCode.CANNOT_EDIT)
    String errCode
) {}
