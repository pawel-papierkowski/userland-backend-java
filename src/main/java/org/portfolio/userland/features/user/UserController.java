package org.portfolio.userland.features.user;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.portfolio.userland.features.user.dto.UserRegisterReq;
import org.portfolio.userland.features.user.service.UserRegisterService;
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
public class UserController {
  private final UserRegisterService userRegisterService;

  /**
   * Tries to register new user.
   * @param userRegisterReq User registration request.
   * @return Result.
   */
  @PostMapping("/register")
  public ResponseEntity<String> registerUser(@Valid @RequestBody UserRegisterReq userRegisterReq) {
    userRegisterService.register(userRegisterReq);
    return new ResponseEntity<>("User registered successfully", HttpStatus.CREATED);
  }
}
