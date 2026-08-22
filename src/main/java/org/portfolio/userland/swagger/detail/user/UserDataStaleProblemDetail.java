package org.portfolio.userland.swagger.detail.user;

import io.swagger.v3.oas.annotations.media.Schema;
import org.portfolio.userland.features.user.constants.UserErrCode;

/**
 * Shows shape of stale data (optimistic locking) error. For Swagger documentation.
 */
@Schema(name = "UserDataStaleProblemDetail", description = "Data was modified in the meantime")
public record UserDataStaleProblemDetail(
    @Schema(example = "https://api.userland.org/errors/user/dataStale")
    String type,
    @Schema(example = "Data was modified in the meantime.")
    String title,
    @Schema(example = "409")
    int status,
    @Schema(example = "User with id '42' was modified by someone else. Please reload data and try again.")
    String detail,
    @Schema(example = "/api/users/edit")
    String instance,
    @Schema(example = UserErrCode.DATA_STALE)
    String errCode
) {}
