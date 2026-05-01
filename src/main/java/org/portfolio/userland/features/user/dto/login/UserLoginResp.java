package org.portfolio.userland.features.user.dto.login;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

/**
 * User login response. Contains basic data about user and token.
 * @param jwtToken JWT token.
 */
@Builder(toBuilder = true)
@Schema(description = "Response for user login.")
public record UserLoginResp(
  @Schema(description = "JWT token.", example = "eyJhbGciOiJIUzI1NiJ9...")
  String jwtToken
) {}
