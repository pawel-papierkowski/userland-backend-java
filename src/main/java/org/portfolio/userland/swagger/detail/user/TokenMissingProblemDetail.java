package org.portfolio.userland.swagger.detail.user;

import io.swagger.v3.oas.annotations.media.Schema;
import org.portfolio.userland.features.user.constants.UserErrCode;

/**
 * Shows shape of token missing error. For Swagger documentation.
 */
@Schema(name = "TokenMissingProblemDetail", description = "Token is missing")
public record TokenMissingProblemDetail(
    @Schema(example = "https://api.userland.org/errors/user/token/missing")
    String type,
    @Schema(example = "User token is missing.")
    String title,
    @Schema(example = "404")
    int status,
    @Schema(example = "Token 'MISSING_TOKEN' does not exist.")
    String detail,
    @Schema(example = "/api/users/activate")
    String instance,
    @Schema(example = UserErrCode.TOKEN_MISSING)
    String errCode
) {}
