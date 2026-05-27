package org.portfolio.userland.features.user.entities;

/**
 * Status of user account.
 */
public enum EnUserStatus {
  /** After creation, but before activation. Old pending users will be removed automatically. */
  PENDING,
  /** Indicates active account. */
  ACTIVE,
  /** Indicates demo account. Used to stuff user table for admin panel viewing. You can interact with such user only
   * in administration panel. */
  DEMO
}
