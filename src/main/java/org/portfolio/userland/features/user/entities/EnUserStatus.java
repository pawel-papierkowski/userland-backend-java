package org.portfolio.userland.features.user.entities;

/**
 * Status of user account.
 */
public enum EnUserStatus {
  /** After creation, but before activate. Old pending users will be removed automatically. */
  PENDING,
  /** Indicates active account. */
  ACTIVE
}
