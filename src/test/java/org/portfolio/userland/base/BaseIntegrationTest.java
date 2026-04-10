package org.portfolio.userland.base;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.portfolio.userland.common.services.clock.ClockService;
import org.portfolio.userland.common.services.clock.MutableClock;
import org.portfolio.userland.utils.problemDetail.ProblemDetailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Base for all integration tests.
 * Provides all stuff commonly needed for this kind of tests, like database handling.
 */
@IntegrationTest
public abstract class BaseIntegrationTest {
  /** Used to simulate HTTP requests. */
  @Autowired
  protected MockMvc mockMvc;
  /** Used to set arbitrary date&time during tests. */
  @Autowired
  protected MutableClock clock;

  /** Service to assert Problem Detail. */
  @Autowired
  protected ProblemDetailService problemDetailService;

  /** Used for initialization of entity fields like createdAt. */
  @Autowired
  protected ClockService clockService;

  /** Used to convert Java objects to JSON. */
  protected final ObjectMapper objectMapper = new ObjectMapper();

  /** Defines PostgreSQL container used in tests. */
  @Container
  @ServiceConnection
  protected static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

  @AfterEach
  protected void resetBaseIntegration() {
    clock.reset();
  }
}
