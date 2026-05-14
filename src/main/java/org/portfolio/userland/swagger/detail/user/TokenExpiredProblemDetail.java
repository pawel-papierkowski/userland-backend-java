package org.portfolio.userland.swagger.detail.user;

import io.swagger.v3.oas.annotations.media.Schema;
import org.portfolio.userland.features.user.constants.UserErrCode;

/**
 * Shows shape of token expired error. For Swagger documentation.
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
    String instance,
    @Schema(example = UserErrCode.TOKEN_EXPIRED)
    String errCode
) {}
