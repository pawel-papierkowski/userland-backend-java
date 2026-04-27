package org.portfolio.userland.common.constants;

import lombok.Getter;

/**
 * Profile of system.
 */
public enum EnAppProfile {
  /** System is running in production environment. */
  PROD(false),
  /** System is running in staging environment. */
  STAGE(true),
  /** System is running in development environment, like locally on dev computer. */
  DEV(true),
  /** System is running in test suite. */
  TEST(true);

  @Getter
  private final Boolean test;

  EnAppProfile(Boolean test) {
    this.test = test;
  }
}
