package org.portfolio.userland.swagger.detail.common;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Shows shape of authorization error. For Swagger documentation.
 */
@Schema(name = "InternalServerErrorProblemDetail", description = "Problem Detail for HTTP 500")
public record InternalServerErrorProblemDetail(
  @Schema(example = "https://api.general.org/errors/internal")
  String type,
  @Schema(example = "Internal Server Error")
  String title,
  @Schema(example = "500")
  int status,
  @Schema(example = "An unexpected error occurred while processing your request.")
  String detail,
  @Schema(example = "/api/checks/exception")
  String instance
) {}
