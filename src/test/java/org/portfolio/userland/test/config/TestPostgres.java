package org.portfolio.userland.test.config;

import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Singleton holder for the PostgreSQL container used across the whole test suite.
 * <p>Ensures the container starts exactly once per JVM and stays alive until it exits (Singleton Container Pattern).
 * This avoids starting multiple competing containers (one per test class) which is slow and resource-hungry.</p>
 */
public final class TestPostgres {

  /** The single shared PostgreSQL container instance. Version pinned to match production database. */
  private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17-alpine");

  private TestPostgres() {
  }

  /**
   * Get the shared container instance, starting it if not yet started.
   * Safe to call multiple times - subsequent calls reuse the running container.
   * @return Shared, running PostgreSQL container.
   */
  public static synchronized PostgreSQLContainer get() {
    if (!POSTGRES.isRunning()) {
      // Started manually instead of via @Container because @Container stops the container after each test class,
      // causing crashes when running the whole test suite.
      POSTGRES.start();
    }
    return POSTGRES;
  }
}
