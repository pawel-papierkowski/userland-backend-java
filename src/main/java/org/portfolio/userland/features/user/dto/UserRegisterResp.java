package org.portfolio.userland.features.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO for user registration. It contains registration response.
 */
@Schema(description = "Response returned after successful registration.")
public record UserRegisterResp (
    @Schema(description = "The unique internal ID of the created user.", example = "1042")
    Long id
) {}
