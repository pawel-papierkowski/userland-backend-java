package org.portfolio.userland.features.user.data;

/**
 * Available token types.
 */
public enum EnTokenType {
  /** Token for activating user after registration. */
  ACTIVATE,
  /** Token for password reset. */
  PASSWORD,
  /** Token for user removal. */
  REMOVAL
}
