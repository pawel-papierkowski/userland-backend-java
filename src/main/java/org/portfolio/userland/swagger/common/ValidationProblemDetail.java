package org.portfolio.userland.swagger.common;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

/**
 * Shows shape of validation error. For Swagger documentation.
 */
@Schema(name = "ValidationProblemDetail", description = "RFC 7807 Problem Detail with validation errors")
public record ValidationProblemDetail(
    @Schema(example = "https://api.general.org/errors/validation")
    String type,
    @Schema(example = "Request Parameter Validation Failed")
    String title,
    @Schema(example = "400")
    int status,
    @Schema(example = "One or more request parameters failed validation.")
    String detail,
    @Schema(example = "/api/users/register")
    String instance,
    @Schema(example = "{\"email\": \"Must be a valid email address\"}")
    Map<String, String> validation_errors
) {}
