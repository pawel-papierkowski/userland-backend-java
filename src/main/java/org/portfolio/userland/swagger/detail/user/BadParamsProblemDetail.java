package org.portfolio.userland.swagger.detail.user;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Shows shape of more complex param error. For Swagger documentation.
 */
@Schema(name = "BadParamsProblemDetail", description = "Invalid param or params combination happened")
public record BadParamsProblemDetail(
    @Schema(example = "https://api.general.org/errors/general/badParams")
    String type,
    @Schema(example = "Bad request.")
    String title,
    @Schema(example = "400")
    int status,
    @Schema(example = "Field createdFromAt is after createdToAt!")
    String detail,
    @Schema(example = "/api/admin/users")
    String instance
) {}
