package org.portfolio.userland.swagger.annotations;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.portfolio.userland.swagger.detail.common.AuthenticationProblemDetail;
import org.portfolio.userland.swagger.detail.common.AuthorizationProblemDetail;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Swagger annotation to indicate endpoints that require only authentication via JWT token.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@ApiResponses(value = {
    @ApiResponse(responseCode = "401", description = "User is not authenticated (no token).",
        content = @Content(mediaType = "application/problem+json",
            schema = @Schema(implementation = AuthenticationProblemDetail.class))),
    @ApiResponse(responseCode = "403", description = "User is not authorized (insufficient permissions).",
        content = @Content(mediaType = "application/problem+json",
            schema = @Schema(implementation = AuthorizationProblemDetail.class)))
})
public @interface ApiResponsesToken {
}
