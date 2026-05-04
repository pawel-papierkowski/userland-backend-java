package org.portfolio.userland.features.user.entities;

/**
 * Available token types.
 */
public enum EnUserTokenType {
  /** Token for activating user after registration. */
  ACTIVATE,
  /** Token for email change. Payload: new email address. */
  EMAIL,
  /** Token for password reset. */
  PASSWORD,
  /** Token for user deletion. */
  DELETE
}
