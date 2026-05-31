package org.portfolio.userland.features.user.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.portfolio.userland.features.user.dto.admin.config.UserConfigTableReq;
import org.portfolio.userland.features.user.dto.admin.config.UserConfigTableResp;
import org.portfolio.userland.features.user.dto.admin.history.UserHistoryTableReq;
import org.portfolio.userland.features.user.dto.admin.history.UserHistoryTableResp;
import org.portfolio.userland.features.user.dto.admin.jwt.UserJwtTableReq;
import org.portfolio.userland.features.user.dto.admin.jwt.UserJwtTableResp;
import org.portfolio.userland.features.user.dto.admin.permission.UserPermissionTableReq;
import org.portfolio.userland.features.user.dto.admin.permission.UserPermissionTableResp;
import org.portfolio.userland.features.user.dto.admin.token.UserTokenTableReq;
import org.portfolio.userland.features.user.dto.admin.token.UserTokenTableResp;
import org.portfolio.userland.features.user.dto.admin.user.UserFullDataReq;
import org.portfolio.userland.features.user.dto.admin.user.UserFullDataResp;
import org.portfolio.userland.features.user.dto.admin.user.UserTableReq;
import org.portfolio.userland.features.user.dto.admin.user.UserTableResp;
import org.portfolio.userland.features.user.services.admin.*;
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
 *   <li><code>POST /api/admin/users/config</code> - get data about config for given user.</li>
 *   <li><code>POST /api/admin/users/history</code> - get data about history for given user.</li>
 *   <li><code>POST /api/admin/users/permissions</code> - get data about permissions for given user.</li>
 *   <li><code>POST /api/admin/users/tokens</code> - get data about tokens for given user.</li>
 *   <li><code>POST /api/admin/users/jwt</code> - get data about JWT for given user.</li>
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
  private final UserConfigTableService userConfigTableService;
  private final UserHistoryTableService userHistoryTableService;
  private final UserPermissionTableService userPermissionTableService;
  private final UserJwtTableService userJwtTableService;
  private final UserTokenTableService userTokenTableService;

  /**
   * View user table data. Request contains filtering and other (pagination, sorting) data needed to return correct
   * results.
   * @param tableReq User table view request.
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
  public ResponseEntity<UserTableResp> viewUserTable(@Valid @RequestBody UserTableReq tableReq) {
    UserTableResp tableResp = userTableService.getPage(tableReq);
    return new ResponseEntity<>(tableResp, HttpStatus.OK);
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

  //

  /**
   * View user config table data. Request contains filtering and other (pagination, sorting) data needed to return
   * correct results.
   * @param tableReq User config table view request.
   * @return Response.
   */
  @PostMapping(value = "/config", produces = "application/json")
  @Operation(summary = "Load user config table", description = "Returns page from user config table. Can be filtered.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Successfully retrieved page from user config table."),
      @ApiResponse(responseCode = "400", description = "Invalid input (e.g., createdFromAt later than createdToAt).",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = BadParamsProblemDetail.class)))
  })
  public ResponseEntity<UserConfigTableResp> viewUserConfigTable(@Valid @RequestBody UserConfigTableReq tableReq) {
    UserConfigTableResp tableResp = userConfigTableService.getPage(tableReq);
    return new ResponseEntity<>(tableResp, HttpStatus.OK);
  }

  //

  /**
   * View user history table data. Request contains filtering and other (pagination, sorting) data needed to return
   * correct results.
   * @param tableReq User config table view request.
   * @return Response.
   */
  @PostMapping(value = "/history", produces = "application/json")
  @Operation(summary = "Load user history table", description = "Returns page from user history table. Can be filtered.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Successfully retrieved page from user history table."),
      @ApiResponse(responseCode = "400", description = "Invalid input (e.g., createdFromAt later than createdToAt).",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = BadParamsProblemDetail.class)))
  })
  public ResponseEntity<UserHistoryTableResp> viewUserHistoryTable(@Valid @RequestBody UserHistoryTableReq tableReq) {
    UserHistoryTableResp tableResp = userHistoryTableService.getPage(tableReq);
    return new ResponseEntity<>(tableResp, HttpStatus.OK);
  }

  //

  /**
   * View user permission table data. Request contains filtering and other (pagination, sorting) data needed to return
   * correct results.
   * @param tableReq User permission table view request.
   * @return Response.
   */
  @PostMapping(value = "/permissions", produces = "application/json")
  @Operation(summary = "Load user permission table", description = "Returns page from user permission table. Can be filtered.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Successfully retrieved page from user permission table."),
      @ApiResponse(responseCode = "400", description = "Invalid input (e.g., createdFromAt later than createdToAt).",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = BadParamsProblemDetail.class)))
  })
  public ResponseEntity<UserPermissionTableResp> viewUserPermissionTable(@Valid @RequestBody UserPermissionTableReq tableReq) {
    UserPermissionTableResp tableResp = userPermissionTableService.getPage(tableReq);
    return new ResponseEntity<>(tableResp, HttpStatus.OK);
  }

  //

  /**
   * View user token table data. Request contains filtering and other (pagination, sorting) data needed to return
   * correct results.
   * @param tableReq User token table view request.
   * @return Response.
   */
  @PostMapping(value = "/tokens", produces = "application/json")
  @Operation(summary = "Load user token table", description = "Returns page from user token table. Can be filtered.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Successfully retrieved page from user token table."),
      @ApiResponse(responseCode = "400", description = "Invalid input (e.g., createdFromAt later than createdToAt).",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = BadParamsProblemDetail.class)))
  })
  public ResponseEntity<UserTokenTableResp> viewUserTokenTable(@Valid @RequestBody UserTokenTableReq tableReq) {
    UserTokenTableResp tableResp = userTokenTableService.getPage(tableReq);
    return new ResponseEntity<>(tableResp, HttpStatus.OK);
  }

  //

  /**
   * View user jwt table data. Request contains filtering and other (pagination, sorting) data needed to return
   * correct results.
   * @param tableReq User jwt table view request.
   * @return Response.
   */
  @PostMapping(value = "/jwt", produces = "application/json")
  @Operation(summary = "Load user jwt table", description = "Returns page from user jwt table. Can be filtered.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Successfully retrieved page from user jwt table."),
      @ApiResponse(responseCode = "400", description = "Invalid input (e.g., createdFromAt later than createdToAt).",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = BadParamsProblemDetail.class)))
  })
  public ResponseEntity<UserJwtTableResp> viewUserJwtTable(@Valid @RequestBody UserJwtTableReq tableReq) {
    UserJwtTableResp tableResp = userJwtTableService.getPage(tableReq);
    return new ResponseEntity<>(tableResp, HttpStatus.OK);
  }
}
