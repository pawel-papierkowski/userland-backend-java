package org.portfolio.userland.common.constants;

import lombok.Getter;

/**
 * <p>System build. You can check if you are on production, development etc. using YAML config <code>app.main.build</code>.</p>
 * <p>Example:</p>
 * <pre>
 *   &#064;Value("${app.main.build}")
 *   protected EnAppBuild build;
 * </pre>
 */
public enum EnAppBuild {
  /** System is running in production environment. */
  PROD(false),
  /** System is running in staging environment. */
  STAGE(true),
  /** System is running in development environment, like locally on dev computer. */
  DEV(true),
  /** System is running in test suite. */
  TEST(true);

  /** If true, this build is considered test build. */
  @Getter
  private final Boolean test;

  EnAppBuild(Boolean test) {
    this.test = test;
  }
}
