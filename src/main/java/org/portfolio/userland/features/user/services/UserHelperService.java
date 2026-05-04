package org.portfolio.userland.features.user.services;

import lombok.RequiredArgsConstructor;
import org.portfolio.userland.features.user.entities.EnUserStatus;
import org.portfolio.userland.features.user.entities.EnUserTokenType;
import org.portfolio.userland.features.user.entities.User;
import org.portfolio.userland.features.user.exceptions.UserInvalidStatusException;
import org.portfolio.userland.features.user.exceptions.UserLockedException;
import org.portfolio.userland.features.user.exceptions.UserNotFoundException;
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
  /** How long before email change token expires in minutes. */
  @Value("${app.user.token.email.expires}")
  private long emailChangeTokenExpires;
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
   * @param failSilently If true, method will return null instead of throwing exception if user with given email does
   *                    not exist.
   * @return User or null if user could not be found.
   */
  public User resolveUser(String email, boolean failSilently) {
    User user;

    if (failSilently) user = userRepository.findByEmail(email).orElse(null);
    else user = userRepository.findByEmail(email).orElseThrow(() -> new UserNotFoundException(email));
    if (user == null) return null; // failed to find user

    if (!verifyUser(user, failSilently)) return null;
    return user;
  }

  /**
   * Verifies user state. If user state is invalid, throws exception or returns false.
   * @param user User.
   * @param failSilently If true, in case of invalid user state return false instead of throwing exception.
   * @return True if verification succeed, otherwise false. Applicable only if <code>failSilently == true</code>.
   */
  public boolean verifyUser(User user, boolean failSilently) {
    if (!EnUserStatus.ACTIVE.equals(user.getStatus())) {
      if (failSilently) return false;
      throw new UserInvalidStatusException(user.getEmail());
    }
    if (user.getLocked()) {
      if (failSilently) return false;
      throw new UserLockedException(user.getEmail());
    }
    return true;
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
      case EMAIL -> nowAt.plusMinutes(emailChangeTokenExpires);
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
