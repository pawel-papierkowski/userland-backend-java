package org.portfolio.userland.features.user.entities;

/**
 * Who caused user history entry?
 */
public enum EnUserHistoryWho {
  /** User caused this. */
  USER,
  /** Operator of admin panel caused this. */
  OPERATOR,
  /** System caused this (no direct action from human). For example, changes from schedulers. */
  SYSTEM
}
