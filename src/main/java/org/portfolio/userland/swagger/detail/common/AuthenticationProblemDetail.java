package org.portfolio.userland.swagger.detail.common;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Shows shape of authentication error. For Swagger documentation.
 */
@Schema(name = "AuthenticationProblemDetail", description = "RFC 7807 Problem Detail for authentication errors")
public record AuthenticationProblemDetail(
    @Schema(example = "https://api.general.org/errors/unauthorized")
    String type,
    @Schema(example = "Unauthorized")
    String title,
    @Schema(example = "401")
    int status,
    @Schema(example = "Authentication is required to access this resource.")
    String detail,
    @Schema(example = "/api/checks/must-be-logged")
    String instance
) {}
