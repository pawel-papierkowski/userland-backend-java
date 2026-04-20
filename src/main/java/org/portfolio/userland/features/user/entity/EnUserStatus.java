package org.portfolio.userland.features.user.entity;

/**
 * Status of user account.
 */
public enum EnUserStatus {
  /** After creation, but before activation. Old pending users will be removed automatically. */
  PENDING,
  /** Indicates active account. */
  ACTIVE
}
