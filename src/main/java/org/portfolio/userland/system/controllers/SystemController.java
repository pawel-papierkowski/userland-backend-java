package org.portfolio.userland.system.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.portfolio.userland.swagger.common.AuthenticationProblemDetail;
import org.portfolio.userland.swagger.common.AuthorizationProblemDetail;
import org.portfolio.userland.system.dto.lockdown.SystemLockdownReq;
import org.portfolio.userland.system.dto.lockdown.SystemLockdownResp;
import org.portfolio.userland.system.services.SystemLockdownService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST endpoints for system management. All endpoints here require administration permissions.
 * <p>Endpoints:</p>
 * <ul>
 *   <li><code>GET /api/system/lockdown</code> - get lockdown status.</li>
 *   <li><code>POST /api/system/lockdown</code> - set lockdown status.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/system")
@RequiredArgsConstructor
@Tag(name = "System Management", description = "Endpoints for general system management.")
public class SystemController {
  private final SystemLockdownService systemLockdownService;

  /**
   * Get status of system lockdown.
   * @return Response.
   */
  @GetMapping(value = "/lockdown", produces = "application/json")
  @Operation(summary = "Status of lockdown", description = "Resolves status of lockdown.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Lockdown status successfully retrieved."),
      @ApiResponse(responseCode = "401", description = "User is not authenticated (no token).",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = AuthenticationProblemDetail.class))),
      @ApiResponse(responseCode = "403", description = "User is not authorized (no ROLE_ADMIN permission).",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = AuthorizationProblemDetail.class)))
  })
  public ResponseEntity<SystemLockdownResp> getLockdown() {
    SystemLockdownResp resp = systemLockdownService.get();
    return new ResponseEntity<>(resp, HttpStatus.OK);
  }

  /**
   * Sets new status of lockdown.
   * @param systemLockdownReq Lockdown request.
   * @return Response.
   */
  @PostMapping(value = "/lockdown", produces = "application/json")
  @Operation(summary = "New status of lockdown", description = "Changes status of lockdown.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Lockdown status successfully changed.",
          content = @Content(schema = @Schema(hidden = true))),
      @ApiResponse(responseCode = "401", description = "User is not authenticated (no token).",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = AuthenticationProblemDetail.class))),
      @ApiResponse(responseCode = "403", description = "User is not authorized (no ROLE_ADMIN permission).",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = AuthorizationProblemDetail.class)))
  })
  public ResponseEntity<String> setLockdown(@Valid @RequestBody SystemLockdownReq systemLockdownReq) {
    systemLockdownService.set(systemLockdownReq);
    return new ResponseEntity<>(HttpStatus.OK);
  }
}
