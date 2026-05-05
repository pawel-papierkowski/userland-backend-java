package org.portfolio.userland.features.user.services;

import lombok.RequiredArgsConstructor;
import org.portfolio.userland.features.user.entities.EnUserStatus;
import org.portfolio.userland.features.user.entities.EnUserTokenType;
import org.portfolio.userland.features.user.entities.User;
import org.portfolio.userland.features.user.exceptions.UserInvalidStatusException;
import org.portfolio.userland.features.user.exceptions.UserLockedException;
import org.portfolio.userland.features.user.exceptions.UserNotFoundException;
import org.portfolio.userland.features.user.exceptions.UserWrongPasswordException;
import org.portfolio.userland.features.user.repositories.UserRepository;
import org.portfolio.userland.system.auth.AuthHelper;
import org.portfolio.userland.system.auth.details.CustomUserDetails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Commonly needed code for user.
 */
@Service
@RequiredArgsConstructor
public class UserHelperService {
  @Autowired
  protected PasswordEncoder passwordEncoder;

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
   * Resolves user by user detail. In other words, user must be logged in.
   * @param failSilently If true, method will return null instead of throwing exception if user does not exist.
   * @return User or null if user could not be found.
   */
  public User resolveUser(boolean failSilently) {
    CustomUserDetails userDetails = AuthHelper.resolveUserDetails();
    if (userDetails == null) {
      if (failSilently) return null;
      throw new IllegalStateException("User details should exist!");
    }
    return resolveUser(userDetails.getEmail(), failSilently);
  }

  /**
   * Resolves user by email and verifies user state.
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
   * Verifies if password is correct. If it is not, throws exception.
   * @param user User data.
   * @param rawPassword Given password.
   */
  public void verifyPassword(User user, String rawPassword) {
    boolean isMatch = passwordEncoder.matches(rawPassword, user.getPassword());
    if (!isMatch) throw new UserWrongPasswordException();
  }

  //

  /**
   * Finds out expiration time.
   * @param type Type of token.
   * @return Time in time units. Check description of given type to see what unit is used (minutes or hours).
   */
  public long resolveExpirationTime(EnUserTokenType type) {
    return switch (type) {
      case ACTIVATE -> activationTokenExpires;
      case EMAIL -> emailChangeTokenExpires;
      case PASSWORD -> passwordResetTokenExpires;
      case DELETE -> deletionTokenExpires;
    };
  }

  /**
   * Finds out when given token type expires.
   * @param nowAt Current date&time.
   * @param type Type of token.
   * @return Expiration date&time.
   */
  public LocalDateTime resolveExpirationSince(LocalDateTime nowAt, EnUserTokenType type) {
    return switch (type) {
      case ACTIVATE -> nowAt.plusHours(activationTokenExpires);
      case EMAIL -> nowAt.plusMinutes(emailChangeTokenExpires);
      case PASSWORD -> nowAt.plusMinutes(passwordResetTokenExpires);
      case DELETE -> nowAt.plusMinutes(deletionTokenExpires);
    };
  }

  /**
   * Finds out when JWT expires.
   * @param issuedAt Issue date&time of JWT.
   * @return Expiration date&time.
   */
  public LocalDateTime resolveJwtExpiration(LocalDateTime issuedAt) {
    return resolveJwtExpiration(issuedAt, null);
  }

  /**
   * Finds out when JWT expires.
   * @param issuedAt Issue date&time of JWT.
   * @param customExpiration Custom expiration period in minutes. Can be null, will use default expiration.
   * @return Expiration date&time.
   */
  public LocalDateTime resolveJwtExpiration(LocalDateTime issuedAt, Long customExpiration) {
    long actualExpiration = jwtExpiration;
    if (customExpiration != null) actualExpiration = customExpiration;
    return issuedAt.plusMinutes(actualExpiration);
  }
}
