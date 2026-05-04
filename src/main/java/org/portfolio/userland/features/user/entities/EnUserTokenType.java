package org.portfolio.userland.features.user.entities;

/**
 * Available token types.
 */
public enum EnUserTokenType {
  /** Token for activating user after registration. Expiration in hours. */
  ACTIVATE,
  /** Token for email change. Expiration in minutes. Payload: new email address. */
  EMAIL,
  /** Token for password reset. Expiration in minutes. */
  PASSWORD,
  /** Token for user deletion. Expiration in minutes. */
  DELETE
}
