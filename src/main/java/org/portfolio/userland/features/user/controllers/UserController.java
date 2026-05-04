package org.portfolio.userland.features.user.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.portfolio.userland.features.user.dto.common.UserDataResp;
import org.portfolio.userland.features.user.dto.delete.UserDeleteConfirmReq;
import org.portfolio.userland.features.user.dto.delete.UserDeleteLinkReq;
import org.portfolio.userland.features.user.dto.edit.UserEditReq;
import org.portfolio.userland.features.user.dto.email.UserEmailChangeConfirmReq;
import org.portfolio.userland.features.user.dto.email.UserEmailChangeLinkReq;
import org.portfolio.userland.features.user.dto.password.UserPassResetConfirmReq;
import org.portfolio.userland.features.user.dto.password.UserPassResetLinkReq;
import org.portfolio.userland.features.user.dto.register.TokenActivateReq;
import org.portfolio.userland.features.user.dto.register.UserRegisterReq;
import org.portfolio.userland.features.user.services.*;
import org.portfolio.userland.swagger.annotations.ApiResponsesAuth;
import org.portfolio.userland.swagger.annotations.ApiResponsesToken;
import org.portfolio.userland.swagger.detail.common.ValidationProblemDetail;
import org.portfolio.userland.swagger.detail.user.TokenAlreadyExistsProblemDetail;
import org.portfolio.userland.swagger.detail.user.TokenMissingProblemDetail;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST endpoints for user. These endpoints are available without authentication.
 * <p>Endpoints:</p>
 * <ul>
 *   <li><code>POST /api/users/register</code> - registers user.</li>
 *   <li><code>POST /api/users/activate</code> - activates user.</li>
 *   <li><code>GET /api/users/view</code> - view user and user profile data.</li>
 *   <li><code>PATCH /api/users/edit</code> - edit user and user profile data.</li>
 *   <li><code>POST /api/users/password/link</code> - sends email with password reset link.</li>
 *   <li><code>PATCH /api/users/password/confirm</code> - actually resets password.</li>
 *   <li><code>POST /api/users/delete/link</code> - sends email with account deletion link.</li>
 *   <li><code>DELETE /api/users/delete/confirm</code> - actually deletes user account.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "Endpoints for creating and managing user accounts.")
public class UserController {
  private final UserRegisterService userRegisterService;
  private final UserViewService userViewService;
  private final UserEditService userEditService;
  private final UserEmailService userEmailService;
  private final UserPasswordService userPasswordService;
  private final UserDeleteService userDeleteService;

  /**
   * Tries to register new user.
   * @param userRegisterReq User registration request.
   * @return Response.
   */
  @PostMapping(value = "/register", produces = "application/json")
  @Operation(summary = "Register a new user", description = "If account with given email does not exist, creates a new user account and sends email with activation link. Otherwise sends email with warning about someone trying to create account.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "201", description = "User successfully registered OR user with that email already exists.",
          content = @Content(schema = @Schema(hidden = true))),
      @ApiResponse(responseCode = "400", description = "Invalid input (e.g., missing email, weak password).",
          content = @Content(mediaType = "application/problem+json",
                             schema = @Schema(implementation = ValidationProblemDetail.class)))
  })
  public ResponseEntity<Void> registerUser(@Valid @RequestBody UserRegisterReq userRegisterReq) {
    userRegisterService.register(userRegisterReq);
    return new ResponseEntity<>(HttpStatus.CREATED);
  }

  /**
   * Fully activates user, provided you give correct token string.
   * @param tokenActivateReq Token activate request.
   * @return Response.
   */
  @PostMapping(value = "/activate", produces = "application/json")
  @Operation(summary = "Activate new user", description = "Calling this endpoint with correct token will fully activate user account.")
  @ApiResponsesToken
  @ApiResponses(value = {
      @ApiResponse(responseCode = "204", description = "User successfully activated.",
          content = @Content(schema = @Schema(hidden = true))),
      @ApiResponse(responseCode = "400", description = "Invalid input (malformed token string).",
          content = @Content(mediaType = "application/problem+json",
                             schema = @Schema(implementation = TokenMissingProblemDetail.class)))
  })
  public ResponseEntity<Void> activateUser(@Valid @RequestBody TokenActivateReq tokenActivateReq) {
    userRegisterService.activate(tokenActivateReq);
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  /**
   * View user account.
   * @return Response with user and user profile data.
   */
  @GetMapping(value = "/view", produces = "application/json")
  @Operation(summary = "View user data", description = "View user and user profile data for currently logged user.")
  @ApiResponsesAuth
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "User and user profile data successfully retrieved.")
  })
  public ResponseEntity<UserDataResp> viewUser() {
    UserDataResp resp = userViewService.view();
    return new ResponseEntity<>(resp, HttpStatus.OK);
  }

  /**
   * Edit user account.
   * @param userEditReq User edit request.
   * @return Response with updated user data.
   */
  @PatchMapping(value = "/edit", produces = "application/json")
  @Operation(summary = "Edit user data", description = "Allows editing certain fields of user or user profile entity for currently logged user.")
  @ApiResponsesAuth
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "User successfully edited."),
      @ApiResponse(responseCode = "400", description = "Invalid input (weak password).",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ValidationProblemDetail.class)))
  })
  public ResponseEntity<UserDataResp> editUser(@Valid @RequestBody UserEditReq userEditReq) {
    UserDataResp resp = userEditService.edit(userEditReq);
    return new ResponseEntity<>(resp, HttpStatus.OK);
  }

  //

  /**
   * Sends emails about email change (warning and link).
   * @param userEmailChangeLinkReq User email change link request.
   * @return Response.
   */
  @PostMapping(value = "/email/link", produces = "application/json")
  @Operation(summary = "Send email change link", description = "Sends emails with warning and link to page where you can change email for your account. Note that trying to use already existing email or wrong password will fail silently to prevent email enumeration attack.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "204", description = "Email change emails successfully sent OR request was ignored due to wrong email address or wrong password.",
          content = @Content(schema = @Schema(hidden = true))),
      @ApiResponse(responseCode = "400", description = "Invalid input (missing or malformed email).",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ValidationProblemDetail.class))),
      @ApiResponse(responseCode = "409", description = "Email change is already pending.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = TokenAlreadyExistsProblemDetail.class)))
  })
  public ResponseEntity<Void> sendEmailChangeLink(@Valid @RequestBody UserEmailChangeLinkReq userEmailChangeLinkReq) {
    userEmailService.send(userEmailChangeLinkReq);
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  /**
   * Actually change email.
   * @param userEmailChangeConfirmReq User email change request.
   * @return Response.
   */
  @PatchMapping(value = "/email/confirm", produces = "application/json")
  @Operation(summary = "Confirm email change", description = "Confirms email change.")
  @ApiResponsesToken
  @ApiResponses(value = {
      @ApiResponse(responseCode = "204", description = "Email change was successful.",
          content = @Content(schema = @Schema(hidden = true))),
      @ApiResponse(responseCode = "400", description = "Invalid input (missing data).",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ValidationProblemDetail.class)))
  })
  public ResponseEntity<Void> emailChangeConfirm(@Valid @RequestBody UserEmailChangeConfirmReq userEmailChangeConfirmReq) {
    userEmailService.confirm(userEmailChangeConfirmReq);
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  //

  /**
   * Sends email with password reset link.
   * @param userPassResetLinkReq User password link request.
   * @return Response.
   */
  @PostMapping(value = "/password/link", produces = "application/json")
  @Operation(summary = "Send password reset link", description = "Sends email with link to page where you can reset password for your account. Note that trying to use unknown email will fail silently to prevent email enumeration attack.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "204", description = "Password reset email successfully sent OR request was ignored due to unknown email address.",
          content = @Content(schema = @Schema(hidden = true))),
      @ApiResponse(responseCode = "400", description = "Invalid input (missing or malformed email).",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ValidationProblemDetail.class))),
      @ApiResponse(responseCode = "409", description = "Password reset is already pending.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = TokenAlreadyExistsProblemDetail.class)))
  })
  public ResponseEntity<Void> sendPasswordResetLink(@Valid @RequestBody UserPassResetLinkReq userPassResetLinkReq) {
    userPasswordService.send(userPassResetLinkReq);
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  /**
   * Actually reset password.
   * @param userPassResetConfirmReq User password reset request.
   * @return Response.
   */
  @PatchMapping(value = "/password/confirm", produces = "application/json")
  @Operation(summary = "Reset password", description = "Reset password.")
  @ApiResponsesToken
  @ApiResponses(value = {
      @ApiResponse(responseCode = "204", description = "Password reset was successful.",
          content = @Content(schema = @Schema(hidden = true))),
      @ApiResponse(responseCode = "400", description = "Invalid input (missing data).",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ValidationProblemDetail.class)))
  })
  public ResponseEntity<Void> passwordResetConfirm(@Valid @RequestBody UserPassResetConfirmReq userPassResetConfirmReq) {
    userPasswordService.reset(userPassResetConfirmReq);
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  //

  /**
   * Sends email with account deletion link.
   * @param userDeleteLinkReq User deletion link request.
   * @return Response.
   */
  @PostMapping(value = "/delete/link", produces = "application/json")
  @Operation(summary = "Send account deletion link", description = "Sends email with link that leads to page where you can confirm account deletion. Note that trying to use unknown email will fail silently to prevent email enumeration attack.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "204", description = "Account deletion email successfully sent OR request was ignored due to unknown email address.",
          content = @Content(schema = @Schema(hidden = true))),
      @ApiResponse(responseCode = "400", description = "Invalid input (missing or malformed email).",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ValidationProblemDetail.class))),
      @ApiResponse(responseCode = "409", description = "Account deletion is already pending.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = TokenAlreadyExistsProblemDetail.class)))
  })
  public ResponseEntity<Void> sendAccountDeleteLink(@Valid @RequestBody UserDeleteLinkReq userDeleteLinkReq) {
    userDeleteService.send(userDeleteLinkReq);
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }

  /**
   * Actually delete user account.
   * @param userDeleteConfirmReq User account deletion request.
   * @return Response.
   */
  @DeleteMapping(value = "/delete/confirm", produces = "application/json")
  @Operation(summary = "Delete user", description = "Removes user from system.")
  @ApiResponsesToken
  @ApiResponses(value = {
      @ApiResponse(responseCode = "204", description = "Account deletion was successful.",
          content = @Content(schema = @Schema(hidden = true))),
      @ApiResponse(responseCode = "400", description = "Invalid input (missing data).",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ValidationProblemDetail.class)))
  })
  public ResponseEntity<Void> accountDeleteConfirm(@Valid @RequestBody UserDeleteConfirmReq userDeleteConfirmReq) {
    userDeleteService.delete(userDeleteConfirmReq);
    return new ResponseEntity<>(HttpStatus.NO_CONTENT);
  }
}
