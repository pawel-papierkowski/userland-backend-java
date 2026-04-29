package org.portfolio.userland.system.lockdown.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.portfolio.userland.swagger.annotations.ApiResponsesAuthPerm;
import org.portfolio.userland.system.lockdown.dto.SystemLockdownReq;
import org.portfolio.userland.system.lockdown.dto.SystemLockdownResp;
import org.portfolio.userland.system.lockdown.services.SystemLockdownService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST endpoints for system lockdown management. All endpoints here require administration permissions.
 * <p>Endpoints:</p>
 * <ul>
 *   <li><code>GET /api/system/lockdown</code> - get lockdown status.</li>
 *   <li><code>POST /api/system/lockdown</code> - set lockdown status.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/system")
@RequiredArgsConstructor
@Tag(name = "System Lockdown Management", description = "Endpoints for system lockdown management. Requires administration permissions.")
public class SystemLockdownController {
  private final SystemLockdownService systemLockdownService;

  /**
   * Get status of system lockdown.
   * @return Response.
   */
  @GetMapping(value = "/lockdown", produces = "application/json")
  @Operation(summary = "Check status of lockdown", description = "Resolves status of lockdown.")
  @ApiResponsesAuthPerm
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Lockdown status successfully retrieved.")
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
  @Operation(summary = "Set new status of lockdown", description = "Changes status of lockdown.")
  @ApiResponsesAuthPerm
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Lockdown status successfully changed.",
          content = @Content(schema = @Schema(hidden = true))),
      @ApiResponse(responseCode = "204", description = "Lockdown status did not change, because it was already in this state.",
          content = @Content(schema = @Schema(hidden = true)))
  })
  public ResponseEntity<Void> setLockdown(@Valid @RequestBody SystemLockdownReq systemLockdownReq) {
    boolean result = systemLockdownService.set(systemLockdownReq);
    if (result) return new ResponseEntity<>(HttpStatus.OK);
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }
}
