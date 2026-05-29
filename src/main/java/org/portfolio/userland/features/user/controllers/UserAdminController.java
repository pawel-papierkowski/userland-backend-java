package org.portfolio.userland.features.user.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.portfolio.userland.features.user.dto.admin.edit.UserFullDataReq;
import org.portfolio.userland.features.user.dto.admin.edit.UserFullDataResp;
import org.portfolio.userland.features.user.dto.admin.view.UserTableReq;
import org.portfolio.userland.features.user.dto.admin.view.UserTableResp;
import org.portfolio.userland.features.user.services.admin.UserTableService;
import org.portfolio.userland.swagger.detail.common.ValidationProblemDetail;
import org.portfolio.userland.swagger.detail.user.BadParamsProblemDetail;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST endpoints for user management. All endpoints here require administration panel access permissions.
 * <p>View endpoints:</p>
 * <ul>
 *   <li><code>POST /api/admin/users</code> - view page of users from table filtered by settings in request.</li>
 *   <li><code>GET /api/admin/users/{id}</code> - get data about single user (basic data and profile).</li>
 *   <li><code>GET /api/admin/users/{id}/history</code> - get data about history for given user.</li>
 *   <li><code>GET /api/admin/users/{id}/tokens</code> - get data about tokens for given user.</li>
 *   <li><code>GET /api/admin/users/{id}/jwt</code> - get data about JWT for given user.</li>
 *   <li><code>GET /api/admin/users/{id}/config</code> - get data about config for given user.</li>
 *   <li><code>GET /api/admin/users/{id}/permissions</code> - get data about permissions for given user.</li>
 * </ul>
 * <p>Edit endpoints:</p>
 * <ul>
 *   <li><code>POST /api/admin/users/{id}</code> - change general and profile data of user with given id.</li>
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
   * @param userTableReq User table view request.
   * @return Response.
   */
  @PostMapping(value = "", produces = "application/json")
  @Operation(summary = "Load user table", description = "Returns page from user table. Can be filtered.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Successfully retrieved page from user table."),
      @ApiResponse(responseCode = "400", description = "Invalid input (e.g., createdFromAt later than createdToAt).",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = BadParamsProblemDetail.class)))
  })
  public ResponseEntity<UserTableResp> viewUserTable(@Valid @RequestBody UserTableReq userTableReq) {
    UserTableResp userTableResp = userTableService.getPage(userTableReq);
    return new ResponseEntity<>(userTableResp, HttpStatus.OK);
  }

  /**
   * View full user data.
   * @return Response.
   */
  @GetMapping(value = "/{id}", produces = "application/json")
  @Operation(summary = "Load user data", description = "Get almost all user and user profile data. It can be any user.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Successfully retrieved user data."),
      @ApiResponse(responseCode = "400", description = "Invalid input (e.g., id is empty).",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ValidationProblemDetail.class)))
  })
  public ResponseEntity<UserFullDataResp> viewUserData(@PathVariable Long id) {
    UserFullDataReq userFullDataReq = UserFullDataReq.builder().id(id).build();
    UserFullDataResp userFullDataResp = userTableService.getUserData(userFullDataReq);
    return new ResponseEntity<>(userFullDataResp, HttpStatus.OK);
  }
}
