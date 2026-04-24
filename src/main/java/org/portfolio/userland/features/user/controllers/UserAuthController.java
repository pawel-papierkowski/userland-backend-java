package org.portfolio.userland.features.user.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.portfolio.userland.features.user.dto.login.UserLoginReq;
import org.portfolio.userland.features.user.dto.login.UserLoginResp;
import org.portfolio.userland.features.user.services.UserLoginService;
import org.portfolio.userland.swagger.common.ValidationProblemDetail;
import org.portfolio.userland.swagger.user.UserWrongPasswordProblemDetail;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoints for user authentication.
 * <p>Endpoints:</p>
 * <ul>
 *   <li><code>POST /api/users/login</code> - user account log in.</li>
 *   <li><code>POST /api/users/logout</code> - user account log out.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User Authentication", description = "Endpoints for login/logout.")
public class UserAuthController {
  private final UserLoginService userLoginService;

  /**
   * Log in user.
   * @param userLoginReq User login request.
   * @return Response.
   */
  @PostMapping(value = "/login", produces = "application/json")
  @Operation(summary = "Login user", description = "Accepts credentials, validates them, logs in user and returns a signed JWT.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Login was successful."),
      @ApiResponse(responseCode = "400", description = "Invalid input (missing data).",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = ValidationProblemDetail.class))),
      @ApiResponse(responseCode = "409", description = "Wrong password.",
          content = @Content(mediaType = "application/problem+json",
              schema = @Schema(implementation = UserWrongPasswordProblemDetail.class)))
  })
  public ResponseEntity<UserLoginResp> login(@Valid @RequestBody UserLoginReq userLoginReq) {
    UserLoginResp resp = userLoginService.login(userLoginReq);
    return new ResponseEntity<>(resp, HttpStatus.OK);
  }

  /**
   * Log out user. Nothing happens if you are already logged out.
   * @return Response.
   */
  @PostMapping(value = "/logout", produces = "application/json")
  @Operation(summary = "Logout user", description = "Perform user logout.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "200", description = "Logout was successful.")
  })
  public ResponseEntity<String> logout() {
    userLoginService.logout();
    return new ResponseEntity<>(HttpStatus.OK);
  }
}
