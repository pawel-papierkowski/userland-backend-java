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
import org.portfolio.userland.features.user.dto.UserRegisterReq;
import org.portfolio.userland.features.user.dto.UserRegisterResp;
import org.portfolio.userland.features.user.services.UserRegisterService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST endpoints for user.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "Endpoints for creating and managing user accounts.")
public class UserController {
  private final UserRegisterService userRegisterService;

  /**
   * Tries to register new user.
   * @param userRegisterReq User registration request.
   * @return Result.
   */
  @PostMapping(value = "/register", produces = "application/json")
  @Operation(summary = "Register a new user", description = "Creates a new user account.")
  @ApiResponses(value = {
      @ApiResponse(responseCode = "201", description = "User successfully registered."),
      @ApiResponse(responseCode = "400", description = "Invalid input (e.g., missing email, weak password).",
          content = @Content(schema = @Schema(hidden = true))),
      @ApiResponse(responseCode = "409", description = "Email already exists in the system.",
          content = @Content(schema = @Schema(hidden = true)))
  })
  public ResponseEntity<UserRegisterResp> registerUser(@Valid @RequestBody UserRegisterReq userRegisterReq) {
    User user = userRegisterService.register(userRegisterReq);
    UserRegisterResp resp = new UserRegisterResp(user.getId());
    return new ResponseEntity<>(resp, HttpStatus.CREATED);
  }
}
