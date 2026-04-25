package org.portfolio.userland.swagger.annotations;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.portfolio.userland.swagger.detail.user.TokenExpiredProblemDetail;
import org.portfolio.userland.swagger.detail.user.TokenMissingProblemDetail;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Swagger annotation for standard token errors.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@ApiResponses(value = {
    @ApiResponse(responseCode = "404", description = "Token does not exist.",
        content = @Content(mediaType = "application/problem+json",
            schema = @Schema(implementation = TokenMissingProblemDetail.class))),
    @ApiResponse(responseCode = "409", description = "Token found, but is expired.",
        content = @Content(mediaType = "application/problem+json",
            schema = @Schema(implementation = TokenExpiredProblemDetail.class)))
})
public @interface ApiResponsesToken {
}
