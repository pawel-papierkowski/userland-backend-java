package org.portfolio.userland.features.user.entity;

/**
 * Available token types.
 */
public enum EnTokenType {
  /** Token for activating user after registration. */
  ACTIVATE,
  /** Token for password reset. */
  PASSWORD,
  /** Token for user deletion. */
  DELETE
}
