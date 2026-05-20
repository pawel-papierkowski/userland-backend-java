package org.portfolio.userland.features.user.dto.login;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

/**
 * User prolong response. Contains JWT.
 * @param jwtToken JWT token.
 */
@Builder(toBuilder = true)
@Schema(description = "Response for user session prolongation.")
public record UserProlongResp(
  @Schema(description = "JWT token.", example = "eyJhbGciOiJIUzI1NiJ9...")
  String jwtToken
) {}
