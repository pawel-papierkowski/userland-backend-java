package org.portfolio.userland.features.user.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.portfolio.userland.features.user.dto.admin.view.UserPageResp;
import org.portfolio.userland.features.user.dto.admin.view.UserTableViewReq;
import org.portfolio.userland.features.user.services.UserTableService;
import org.portfolio.userland.swagger.detail.common.ValidationProblemDetail;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoints for user management. All endpoints here require administration panel access permissions.
 * <p>Endpoints:</p>
 * <ul>
 *   <li><code>GET /api/admin/users</code> - view page of users from table filtered by settings in request.</li>
 *   <li><code>GET /api/admin/user/{id}</code> - get data about single user.</li>
 *   <li><code>GET /api/admin/user/{id}/history</code> - get data about history for given user.</li>
 *   <li><code>GET /api/admin/user/{id}/tokens</code> - get data about tokens for given user.</li>
 *   <li><code>GET /api/admin/user/{id}/jwt</code> - get data about JWT for given user.</li>
 *   <li><code>GET /api/admin/user/{id}/config</code> - get data about config for given user.</li>
 *   <li><code>GET /api/admin/user/{id}/permissions</code> - get data about permissions for given user.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "Endpoints for managing user accounts. Requires administration panel access permissions.")
public class UserAdminController {
  private final UserTableService userTableService;

  /**
   * View user table data. Request contains filtering and other (pagination, sorting) data needed to return correct
   * results.
   * @param userTableViewReq User table view request.
   * @return Response.
   */
  @PostMapping(value = "", produces = "application/json")
  @Operation(summary = "Load user table", description = "Returns page from user table.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Successfully retrieved page from user table.",
          content = @Content(schema = @Schema(hidden = true))),
      @ApiResponse(responseCode = "400", description = "Invalid input (e.g., createdFromAt later than createdToAt).",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ValidationProblemDetail.class)))
  })
  public ResponseEntity<UserPageResp> viewUserTable(@Valid @RequestBody UserTableViewReq userTableViewReq) {
    UserPageResp userPageResp = userTableService.getPage(userTableViewReq);
    return new ResponseEntity<>(userPageResp, HttpStatus.OK);
  }
}
