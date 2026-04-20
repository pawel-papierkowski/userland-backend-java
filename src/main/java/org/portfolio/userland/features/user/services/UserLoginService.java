package org.portfolio.userland.features.user.services;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.portfolio.userland.common.services.jwt.JwtService;
import org.portfolio.userland.features.user.dto.login.UserLoginReq;
import org.portfolio.userland.features.user.dto.login.UserLoginResp;
import org.portfolio.userland.features.user.entity.EnHistoryWhat;
import org.portfolio.userland.features.user.entity.User;
import org.portfolio.userland.features.user.exception.UserWrongPasswordException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Handles user login and logout.
 */
@Service
@RequiredArgsConstructor
public class UserLoginService extends BaseUserService {
  private final JwtService jwtService;
  private final PasswordEncoder passwordEncoder;

  /**
   * Perform user login.
   * @param userLoginReq User login request.
   * @return User login response.
   */
  @Transactional
  public UserLoginResp login(@Valid UserLoginReq userLoginReq) {
    User user = resolveUser(userLoginReq.email());
    verifyPassword(user, userLoginReq.password());

    LocalDateTime nowAt = clockService.getNowUTC();
    user.addHistory(createHistoryEvent(nowAt, EnHistoryWhat.LOGIN));

    // Login is successful, generate JWT token now.
    String jwtToken = jwtService.generateToken(user);
    return new UserLoginResp(jwtToken);
  }

  /**
   * Verifies if password is correct. If it is not, throws exception.
   * @param user User data.
   * @param rawPassword Given password.
   */
  private void verifyPassword(User user, String rawPassword) {
    boolean isMatch = passwordEncoder.matches(rawPassword, user.getPassword());
    if (!isMatch) throw new UserWrongPasswordException(user.getEmail());
  }
}
