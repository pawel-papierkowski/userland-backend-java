package org.portfolio.userland.features.check.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.portfolio.userland.swagger.annotations.ApiResponsesAuth;
import org.portfolio.userland.swagger.annotations.ApiResponsesToken;
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
 *   <li><code>/api/checks/alive</code> - anyone can access this endpoint.</li>
 *   <li><code>/api/checks/must-be-logged</code> - needs to be logged to access this endpoint.</li>
 *   <li><code>/api/checks/must-be-admin</code> - needs to be logged as admin to access this endpoint.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/checks")
@RequiredArgsConstructor
@Tag(name = "Checks", description = "Endpoints for assessing state of server and help in frontend development.")
public class CheckController {

  /**
   * Simply indicates this server is alive. No access restrictions: anyone can call this endpoint, unlogged or logged.
   * @return Response.
   */
  @GetMapping(value = "/alive", produces = "application/json")
  @Operation(summary = "I am alive", description = "Shows that server is up.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Server is up.",
          content = @Content(schema = @Schema(hidden = true))),
  })
  public ResponseEntity<String> alive() {
    // Yes, this endpoint does nothing by itself.
    return new ResponseEntity<>(HttpStatus.OK);
  }

  /**
   * Checks if you can access endpoint that requires you to be logged in. No other permissions required.
   * @return Response.
   */
  @GetMapping(value = "/must-be-logged", produces = "application/json")
  @Operation(summary = "Must be logged", description = "You need to be logged in to successfully access this endpoint. Does nothing else.")
  @ApiResponsesToken
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Access was successful.",
          content = @Content(schema = @Schema(hidden = true)))
  })
  public ResponseEntity<String> mustBeLogged() {
    // Yes, this endpoint does nothing by itself.
    return new ResponseEntity<>(HttpStatus.OK);
  }

  /**
   * Checks if you can access endpoint that requires you to be logged in as user with certain permission (ROLE_ADMIN).
   * Tests permissions.
   * @return Response.
   */
  @GetMapping(value = "/must-be-admin", produces = "application/json")
  @PreAuthorize("hasAuthority('ROLE_ADMIN')")
  @Operation(summary = "Must be admin", description = "You need to be logged in as admin (ROLE_ADMIN) to successfully access this endpoint. Does nothing else.")
  @ApiResponsesAuth
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Access was successful.",
          content = @Content(schema = @Schema(hidden = true)))
  })
  public ResponseEntity<String> mustBeAdmin() {
    // Yes, this endpoint does nothing by itself.
    return new ResponseEntity<>(HttpStatus.OK);
  }
}
