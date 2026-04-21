package org.portfolio.userland.features.user.services;

import lombok.RequiredArgsConstructor;
import org.portfolio.userland.features.user.entity.EnTokenType;
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

  /**
   * Finds out when given token type expires.
   * @param nowAt Current date&time.
   * @param type Type of token.
   * @return Expiration date&time.
   */
  public LocalDateTime resolveExpiration(LocalDateTime nowAt, EnTokenType type) {
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
