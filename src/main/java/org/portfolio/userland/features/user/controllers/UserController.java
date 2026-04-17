package org.portfolio.userland.features.user.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.portfolio.userland.features.user.data.User;
import org.portfolio.userland.features.user.dto.activate.TokenActivateReq;
import org.portfolio.userland.features.user.dto.delete.UserDeleteConfirmReq;
import org.portfolio.userland.features.user.dto.delete.UserDeleteLinkReq;
import org.portfolio.userland.features.user.dto.password.UserPassLinkReq;
import org.portfolio.userland.features.user.dto.password.UserPassResetReq;
import org.portfolio.userland.features.user.dto.register.UserRegisterReq;
import org.portfolio.userland.features.user.dto.register.UserRegisterResp;
import org.portfolio.userland.features.user.services.UserDeleteService;
import org.portfolio.userland.features.user.services.UserPasswordService;
import org.portfolio.userland.features.user.services.UserRegisterService;
import org.portfolio.userland.swagger.user.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoints for user.
 * <p>Endpoints:</p>
 * <ul>
 *   <li><code>/api/users/register</code> - registers user.</li>
 *   <li><code>/api/users/activate</code> - activates user.</li>
 *   <li><code>/api/users/password/send - sends email with password reset link.</code></li>
 *   <li><code>/api/users/password/reset</code> - actually resets password.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "Endpoints for creating and managing user accounts.")
public class UserController {
  private final UserRegisterService userRegisterService;
  private final UserPasswordService userPasswordService;
  private final UserDeleteService userDeleteService;

  /**
   * Tries to register new user.
   * @param userRegisterReq User registration request.
   * @return Response.
   */
  @PostMapping(value = "/register", produces = "application/json")
  @Operation(summary = "Register a new user", description = "Creates a new user account.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "201", description = "User successfully registered."),
      @ApiResponse(responseCode = "400", description = "Invalid input (e.g., missing email, weak password).",
          content = @Content(mediaType = "application/problem+json",
                             schema = @Schema(implementation = ValidationProblemDetail.class))),
      @ApiResponse(responseCode = "409", description = "Email already exists in the system.",
          content = @Content(mediaType = "application/problem+json",
                             schema = @Schema(implementation = EmailExistsProblemDetail.class)))
  })
  public ResponseEntity<UserRegisterResp> registerUser(@Valid @RequestBody UserRegisterReq userRegisterReq) {
    User user = userRegisterService.register(userRegisterReq);
    UserRegisterResp resp = new UserRegisterResp(user.getId());
    return new ResponseEntity<>(resp, HttpStatus.CREATED);
  }

  /**
   * Fully activates user, provided you give correct token string.
   * @param tokenActivateReq Token activation request.
   * @return Response.
   */
  @PostMapping(value = "/activate", produces = "application/json")
  @Operation(summary = "Activate new user", description = "Calling this endpoint with correct token will fully activate user account.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "User successfully activated.",
          content = @Content(schema = @Schema(hidden = true))),
      @ApiResponse(responseCode = "404", description = "Invalid input (malformed token string) or token does not exist.",
          content = @Content(mediaType = "application/problem+json",
                             schema = @Schema(implementation = TokenMissingProblemDetail.class))),
      @ApiResponse(responseCode = "409", description = "Token found, but is expired.",
          content = @Content(mediaType = "application/problem+json",
                             schema = @Schema(implementation = TokenExpiredProblemDetail.class)))
  })
  public ResponseEntity<String> activateUser(@Valid @RequestBody TokenActivateReq tokenActivateReq) {
    userRegisterService.activate(tokenActivateReq);
    return new ResponseEntity<>(HttpStatus.OK);
  }

  //

  /**
   * Sends email with password reset link.
   * @param userPassLinkReq User password link request.
   * @return Response.
   */
  @PostMapping(value = "/password/send", produces = "application/json")
  @Operation(summary = "Send password reset link", description = "Sends email with link to page where you can reset password to your account.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Email successfully sent."),
      @ApiResponse(responseCode = "400", description = "Invalid input (missing or malformed email).",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ValidationProblemDetail.class))),
      @ApiResponse(responseCode = "404", description = "User with given email does not exist.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = UserDoesNotExistProblemDetail.class))),
      @ApiResponse(responseCode = "409", description = "User is not allowed to reset password (e.g. not activated or locked).",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ProblemDetail.class)))
  })
  public ResponseEntity<String> sendPasswordResetLink(@Valid @RequestBody UserPassLinkReq userPassLinkReq) {
    userPasswordService.send(userPassLinkReq);
    return new ResponseEntity<>(HttpStatus.OK);
  }

  /**
   * Actually reset password.
   * @param userPassResetReq User password reset request.
   * @return Response.
   */
  @PostMapping(value = "/password/reset", produces = "application/json")
  @Operation(summary = "Reset password", description = "Reset password.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Password reset successfully."),
      @ApiResponse(responseCode = "400", description = "Invalid input (missing data).",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ValidationProblemDetail.class))),
      @ApiResponse(responseCode = "404", description = "Token does not exist.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = TokenMissingProblemDetail.class)))
  })
  public ResponseEntity<String> passwordReset(@Valid @RequestBody UserPassResetReq userPassResetReq) {
    userPasswordService.reset(userPassResetReq);
    return new ResponseEntity<>(HttpStatus.OK);
  }

  //

  /**
   * Sends email with account deletion link.
   * @param userDeleteLinkReq User deletion link request.
   * @return Response.
   */
  @PostMapping(value = "/delete/send", produces = "application/json")
  @Operation(summary = "Send account deletion link", description = "Sends email with link that leads to page where you can confirm account deletion.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Email successfully sent."),
      @ApiResponse(responseCode = "400", description = "Invalid input (missing or malformed email).",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ValidationProblemDetail.class))),
      @ApiResponse(responseCode = "404", description = "User with given email does not exist.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = UserDoesNotExistProblemDetail.class))),
      @ApiResponse(responseCode = "409", description = "User is not allowed to delete account (e.g. not activated or locked).",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ProblemDetail.class)))
  })
  public ResponseEntity<String> sendAccountDeleteLink(@Valid @RequestBody UserDeleteLinkReq userDeleteLinkReq) {
    userDeleteService.send(userDeleteLinkReq);
    return new ResponseEntity<>(HttpStatus.OK);
  }

  /**
   * Actually delete user account.
   * @param userDeleteConfirmReq User account deletion request.
   * @return Response.
   */
  @PostMapping(value = "/delete/confirm", produces = "application/json")
  @Operation(summary = "Delete user", description = "Removes user from system.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Account deletion was successful."),
      @ApiResponse(responseCode = "400", description = "Invalid input (missing data).",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ValidationProblemDetail.class))),
      @ApiResponse(responseCode = "404", description = "Token does not exist.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = TokenMissingProblemDetail.class)))
  })
  public ResponseEntity<String> accountDeleteConfirm(@Valid @RequestBody UserDeleteConfirmReq userDeleteConfirmReq) {
    userDeleteService.delete(userDeleteConfirmReq);
    return new ResponseEntity<>(HttpStatus.OK);
  }
}
