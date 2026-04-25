package org.portfolio.userland.system.history.entity;

/**
 * Who caused system history event?
 */
public enum EnHistoryWho {
  /** Operator of admin panel caused this. */
  OPERATOR,
  /** Administrator caused this. */
  ADMIN,
  /** System caused this (no direct action from human). For example, changes from schedulers. */
  SYSTEM
}
