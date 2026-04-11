package org.portfolio.userland.features.user.data;

/**
 * What caused user history entry?
 */
public enum EnHistoryWhat {
  /** User was created. */
  CREATED,
  /** User was activated. */
  ACTIVATED,
  /** Password reset was requested. */
  PASS_RESET,
  /** User logged in. */
  LOGIN
}
