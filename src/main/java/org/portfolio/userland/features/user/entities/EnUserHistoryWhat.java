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
  /** Email change was requested. */
  EMAIL_CHANGE_REQ,
  /** Email change was done. */
  EMAIL_CHANGE,
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
