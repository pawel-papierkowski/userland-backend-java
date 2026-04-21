package org.portfolio.userland.features.user.services;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.portfolio.userland.common.services.jwt.JwtService;
import org.portfolio.userland.common.services.security.UserLandDetails;
import org.portfolio.userland.features.user.dto.login.UserLoginReq;
import org.portfolio.userland.features.user.dto.login.UserLoginResp;
import org.portfolio.userland.features.user.entity.EnHistoryWhat;
import org.portfolio.userland.features.user.entity.User;
import org.portfolio.userland.features.user.exception.UserWrongPasswordException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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

    // Login is successful. Generate JWT token now.
    String jwtToken = jwtService.generateToken(user);
    // Add JWT in database. This will allow us to effectively revoke tokens later (logout etc).
    user.addJwt(createJwtEntry(nowAt, jwtToken));
    userRepository.save(user);

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

  // //////////////////////////////////////////////////////////////////////////

  /**
   * Perform user logout. If there is no login, nothing happens.
   */
  @Transactional
  public void logout() {
    // If we aren't logged in, just end. Nothing to do.
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()) return;
    Object principalObj = authentication.getPrincipal();
    // Can happen if we call endpoints that do not handle jwtAuthFilter. See SecurityConfig.
    if (!(principalObj instanceof UserLandDetails)) return;

    UserLandDetails principal = (UserLandDetails) authentication.getPrincipal();

    // If we are logged in, add entry in history and remove token in database.
    LocalDateTime nowAt = clockService.getNowUTC();
    User user = resolveUser(principal.getEmail());
    user.addHistory(createHistoryEvent(nowAt, EnHistoryWhat.LOGOUT));
    user.getJwt().clear(); // Revoke all JWT related to this user.
    userRepository.save(user);
  }
}
