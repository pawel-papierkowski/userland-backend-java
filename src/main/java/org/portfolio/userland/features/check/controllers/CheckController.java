package org.portfolio.userland.features.check.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.portfolio.userland.features.check.data.CheckInfoResp;
import org.portfolio.userland.features.check.services.CheckService;
import org.portfolio.userland.swagger.annotations.ApiResponsesAuth;
import org.portfolio.userland.swagger.annotations.ApiResponsesAuthPerm;
import org.portfolio.userland.swagger.detail.common.InternalServerErrorProblemDetail;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints for assessing state of server and help in frontend development. Also, useful for some tests.
 * <p>Endpoints:</p>
 * <ul>
 *   <li><code>GET /api/checks/alive</code> - anyone can access this endpoint.</li>
 *   <li><code>GET /api/checks/must-be-logged</code> - needs to be logged to access this endpoint.</li>
 *   <li><code>GET /api/checks/must-be-admin</code> - needs to be logged as admin to access this endpoint.</li>
 *   <li><code>GET /api/checks/info</code> - basic information about system.</li>
 *   <li><code>GET /api/checks/exception</code> - code deliberately throws exception. Response returns proper problem detail.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/checks")
@RequiredArgsConstructor
@Tag(name = "Checks", description = "Endpoints for assessing state of server and help in frontend development.")
public class CheckController {
  private final CheckService checkService;

  /**
   * Simply indicates this server is alive. No access restrictions: anyone can call this endpoint, unlogged or logged.
   * @return Response.
   */
  @GetMapping(value = "/alive", produces = "application/json")
  @Operation(summary = "I am alive", description = "Shows that server is up.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "204", description = "Server is up.",
          content = @Content(schema = @Schema(hidden = true))),
  })
  public ResponseEntity<Void> alive() {
    // Yes, this endpoint does nothing by itself.
    return new ResponseEntity<>(HttpStatus.NO_CONTENT); // deliberately OK instead of NO_CONTENT in this particular case
  }

  /**
   * Checks if you can access endpoint that requires you to be logged in. No other permissions required.
   * @return Response.
   */
  @GetMapping(value = "/must-be-logged", produces = "application/json")
  @Operation(summary = "Must be logged", description = "You need to be logged in to successfully access this endpoint. Does nothing else.")
  @ApiResponsesAuth
  @ApiResponses(value = {
      @ApiResponse(responseCode = "204", description = "Access was successful.",
          content = @Content(schema = @Schema(hidden = true)))
  })
  public ResponseEntity<Void> mustBeLogged() {
    // Yes, this endpoint does nothing by itself.
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  /**
   * Checks if you can access endpoint that requires you to be logged in as user with certain permission (ROLE_ADMIN).
   * Tests permissions.
   * @return Response.
   */
  @GetMapping(value = "/must-be-admin", produces = "application/json")
  @PreAuthorize("hasAuthority('ROLE_ADMIN')")
  @Operation(summary = "Must be admin", description = "You need to be logged in as admin (ROLE_ADMIN) to successfully access this endpoint. Does nothing else.")
  @ApiResponsesAuthPerm
  @ApiResponses(value = {
      @ApiResponse(responseCode = "204", description = "Access was successful.",
          content = @Content(schema = @Schema(hidden = true)))
  })
  public ResponseEntity<Void> mustBeAdmin() {
    // Yes, this endpoint does nothing by itself.
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  //

  /**
   * Returns basic information about this server like server date&time or profile.
   * @return Response.
   */
  @GetMapping(value = "/info", produces = "application/json")
  @PreAuthorize("hasAuthority('ROLE_ADMIN')")
  @Operation(summary = "Information", description = "Returns various data about system.")
  @ApiResponsesAuthPerm
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Access was successful.")
  })
  public ResponseEntity<CheckInfoResp> info() {
    CheckInfoResp info = checkService.info();
    return new ResponseEntity<>(info, HttpStatus.OK);
  }

  /**
   * Deliberately throws exception.
   * @return Response.
   */
  @GetMapping(value = "/exception", produces = "application/json")
  @Operation(summary = "Throws exception", description = "Returns error.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "500", description = "Deliberate error.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = InternalServerErrorProblemDetail.class)))
  })
  public ResponseEntity<Void> error() {
    // Yes, this endpoint does nothing by itself.
    throw new IllegalArgumentException("This error message was caused deliberately (/api/checks/exception) and should not be visible externally.");
  }
}
