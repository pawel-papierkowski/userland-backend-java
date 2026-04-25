package org.portfolio.userland.swagger.detail.common;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Shows shape of authorization error. For Swagger documentation.
 */
@Schema(name = "AuthorizationProblemDetail", description = "RFC 7807 Problem Detail for authorization errors")
public record AuthorizationProblemDetail(
    @Schema(example = "https://api.general.org/errors/forbidden")
    String type,
    @Schema(example = "Forbidden")
    String title,
    @Schema(example = "403")
    int status,
    @Schema(example = "You do not have permission to access this resource.")
    String detail,
    @Schema(example = "/api/checks/must-be-admin")
    String instance
) {}
