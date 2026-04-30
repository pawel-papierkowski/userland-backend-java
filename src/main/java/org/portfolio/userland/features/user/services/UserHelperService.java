package org.portfolio.userland.features.user.services;

import lombok.RequiredArgsConstructor;
import org.portfolio.userland.features.user.entities.EnUserStatus;
import org.portfolio.userland.features.user.entities.EnUserTokenType;
import org.portfolio.userland.features.user.entities.User;
import org.portfolio.userland.features.user.exceptions.UserDoesNotExistException;
import org.portfolio.userland.features.user.exceptions.UserInvalidStatusException;
import org.portfolio.userland.features.user.exceptions.UserLockedException;
import org.portfolio.userland.features.user.repositories.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Commonly needed code for user.
 */
@Service
@RequiredArgsConstructor
public class UserHelperService {
  /** How long before activation token expires in hours. */
  @Value("${app.user.token.activation.expires}")
  private long activationTokenExpires;
  /** How long before password reset token expires in minutes. */
  @Value("${app.user.token.password.expires}")
  private long passwordResetTokenExpires;
  /** How long before account deletion token expires in minutes. */
  @Value("${app.user.token.deletion.expires}")
  private long deletionTokenExpires;

  /** How long before JWT token expires in minutes. */
  @Value("${security.jwt.expiration}")
  private long jwtExpiration;

  private final UserRepository userRepository;

  //

  /**
   * Resolves user and verifies user state.
   * @param email User email.
   * @return User.
   */
  public User resolveUser(String email) {
    User user = userRepository.findByEmail(email).orElseThrow(() -> new UserDoesNotExistException(email));
    verifyUser(user);
    return user;
  }

  /**
   * Verifies user state. If user state is invalid, throws exception.
   * @param user User.
   */
  public void verifyUser(User user) {
    if (!EnUserStatus.ACTIVE.equals(user.getStatus())) throw new UserInvalidStatusException(user.getEmail());
    if (user.getLocked()) throw new UserLockedException(user.getEmail());
  }

  //

  /**
   * Finds out when given token type expires.
   * @param nowAt Current date&time.
   * @param type Type of token.
   * @return Expiration date&time.
   */
  public LocalDateTime resolveExpiration(LocalDateTime nowAt, EnUserTokenType type) {
    return switch (type) {
      case ACTIVATE -> nowAt.plusHours(activationTokenExpires);
      case PASSWORD -> nowAt.plusMinutes(passwordResetTokenExpires);
      case DELETE -> nowAt.plusMinutes(deletionTokenExpires);
    };
  }

  /**
   * Finds out when JWT expires.
   * @param nowAt Current date&time.
   * @return Expiration date&time.
   */
  public LocalDateTime resolveJwtExpiration(LocalDateTime nowAt) {
    return nowAt.plusMinutes(jwtExpiration);
  }
}
