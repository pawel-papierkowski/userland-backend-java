package org.portfolio.userland.common.constants;

import lombok.Getter;

/**
 * <p>System profile. You can check if you are on production, development etc. using YAML config <code>app.main.profile</code>.</p>
 * <p>Example:</p>
 * <pre>
 *   &#064;Value("${app.main.profile}")
 *   protected EnAppProfile profile;
 * </pre>
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

  /** If true, this profile is considered test profile. */
  @Getter
  private final Boolean test;

  EnAppProfile(Boolean test) {
    this.test = test;
  }
}
