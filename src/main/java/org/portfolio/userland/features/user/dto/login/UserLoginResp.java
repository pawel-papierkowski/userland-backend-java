package org.portfolio.userland.features.user.dto.login;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * User login response.
 * @param jwtToken JWT token.
 */
@Schema(description = "Response for user login.")
public record UserLoginResp(
  @Schema(description = "JWT token.", example = "eyJhbGciOiJIUzI1NiJ9...")
  String jwtToken
) {}
