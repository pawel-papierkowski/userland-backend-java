package org.portfolio.userland.features.user.entities;

/**
 * What caused user history entry?
 */
public enum EnUserHistoryWhat {
  /** User was created. */
  CREATE,
  /** User was activated. */
  ACTIVATE,
  /** User edited their account. */
  EDIT,
  /** Password reset was requested. */
  PASS_RESET_REQ,
  /** Password reset was done. */
  PASS_RESET,
  /** Account deletion was requested. */
  DELETE_REQ,
  /** User logged in. */
  LOGIN,
  /** User logged out. */
  LOGOUT
}
