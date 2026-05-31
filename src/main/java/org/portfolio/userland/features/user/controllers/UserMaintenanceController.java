package org.portfolio.userland.features.user.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.portfolio.userland.common.services.lock.LockService;
import org.portfolio.userland.features.user.constants.UserLockConst;
import org.portfolio.userland.features.user.schedulers.UserScheduler;
import org.portfolio.userland.features.user.services.standard.UserMaintenanceService;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoints for user maintenance. All endpoints here are equivalents of corresponding entries in <code>UserScheduler</code>.
 * All endpoints are secured against running more than one task at once. You need to be admin.
 * <p>Endpoints:</p>
 * <ul>
 *   <li><code>POST /api/users/maintenance/expiredUsers</code> - cleanup of expired users.</li>
 *   <li><code>POST /api/users/maintenance/expiredTokens</code> - cleanup of expired tokens.</li>
 *   <li><code>POST /api/users/maintenance/expiredJwts</code> - cleanup of expired JWTs.</li>
 * </ul>
 * @see UserScheduler
 */
@RestController
@RequestMapping("/api/users/maintenance")
@RequiredArgsConstructor
@Tag(name = "User Maintenance", description = "Endpoints for manual user maintenance.")
public class UserMaintenanceController {
  private final UserMaintenanceService userMaintenanceService;
  private final LockService lockService;

  /**
   * Cleanup of expired pending users.
   * @return Response.
   */
  @PostMapping(value = "/pendingUsers", produces = "application/json")
  @PreAuthorize("hasAuthority('ROLE_ADMIN')")
  @Operation(summary = "Cleanup of pending users", description = "Remove all PENDING users that are too old.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Successfully started cleanup.",
          content = @Content(schema = @Schema(hidden = true))),
      @ApiResponse(responseCode = "423", description = "Locked: same code is still running.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ProblemDetail.class)))
  })
  public ResponseEntity<Void> cleanPendingUsers() {
    return lockService.endpointWithLock(UserLockConst.CLEAN_PENDING_USERS, userMaintenanceService::cleanPendingUsers);
  }

  /**
   * Cleanup of expired active users.
   * @return Response.
   */
  @PostMapping(value = "/activeUsers", produces = "application/json")
  @PreAuthorize("hasAuthority('ROLE_ADMIN')")
  @Operation(summary = "Cleanup of active users", description = "Remove all ACTIVE users that were idle for too long. Note: works only in portfolio mode.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Successfully started cleanup.",
          content = @Content(schema = @Schema(hidden = true))),
      @ApiResponse(responseCode = "423", description = "Locked: same code is still running.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ProblemDetail.class)))
  })
  public ResponseEntity<Void> cleanExpiredUsers() {
    return lockService.endpointWithLock(UserLockConst.CLEAN_ACTIVE_USERS, userMaintenanceService::cleanActiveUsers);
  }

  /**
   * Cleanup of expired tokens.
   * @return Response.
   */
  @PostMapping(value = "/expiredTokens", produces = "application/json")
  @PreAuthorize("hasAuthority('ROLE_ADMIN')")
  @Operation(summary = "Cleanup of expired tokens", description = "Remove all tokens that are too old.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Successfully started cleanup.",
          content = @Content(schema = @Schema(hidden = true))),
      @ApiResponse(responseCode = "423", description = "Locked: same code is still running.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ProblemDetail.class)))
  })
  public ResponseEntity<Void> cleanExpiredTokens() {
    return lockService.endpointWithLock(UserLockConst.CLEAN_EXPIRED_TOKENS, userMaintenanceService::cleanExpiredTokens);
  }

  /**
   * Cleanup of expired JWTs.
   * @return Response.
   */
  @PostMapping(value = "/expiredJwts", produces = "application/json")
  @PreAuthorize("hasAuthority('ROLE_ADMIN')")
  @Operation(summary = "Cleanup of expired JWTs", description = "Remove all JWTs that are too old.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Successfully started cleanup.",
          content = @Content(schema = @Schema(hidden = true))),
      @ApiResponse(responseCode = "423", description = "Locked: same code is still running.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ProblemDetail.class)))
  })
  public ResponseEntity<Void> cleanExpiredJwts() {
    return lockService.endpointWithLock(UserLockConst.CLEAN_EXPIRED_JWTS, userMaintenanceService::cleanExpiredJwts);
  }
}
