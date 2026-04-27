package org.portfolio.userland.test.base;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.portfolio.userland.common.services.clock.ClockService;
import org.portfolio.userland.common.services.clock.MutableClock;
import org.portfolio.userland.system.config.entities.Config;
import org.portfolio.userland.system.config.repositories.ConfigRepository;
import org.portfolio.userland.system.config.service.ConfigConst;
import org.portfolio.userland.system.history.repositories.SystemHistoryRepository;
import org.portfolio.userland.test.helpers.problemDetail.ProblemDetailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Base class for all integration tests.
 * Provides all stuff commonly needed for this kind of tests, like database handling.
 * Note we use container in Singleton Container Pattern here (without @Testcontainers) because it causes issues when
 * running test suite (multiple test files).
 */
@IntegrationTest
public abstract class BaseIntegrationTest {
  /** Project config variables. */
  @Autowired
  protected ConfigRepository configRepository;
  /** System history events. */
  @Autowired
  protected SystemHistoryRepository systemHistoryRepository;

  /** Used to simulate HTTP requests. */
  @Autowired
  protected MockMvc mockMvc;
  /** Used to set arbitrary date&time during tests. */
  @Autowired
  protected MutableClock clock;
  /** For specific tasks like flushing cache. */
  @Autowired
  protected EntityManager entityManager;

  @Autowired
  protected TransactionTemplate transactionTemplate;

  /** Service to assert Problem Detail. */
  @Autowired
  protected ProblemDetailService problemDetailService;

  /** Used for initialization of entity fields like createdAt. */
  @Autowired
  protected ClockService clockService;

  /** Used to convert Java objects to JSON. Note: for some reason autowiring ObjectMapper fails. */
  protected final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

  /** Defines PostgreSQL container used in tests. Note we manage it manually. */
  @ServiceConnection
  protected static final PostgreSQLContainer postgresCont = new PostgreSQLContainer("postgres:17-alpine");

  //

  static {
    // This ensures the container starts exactly once for the entire test suite run, and stays alive until the JVM exits.
    // Use of @Container would cause crashes when running test suite.
    postgresCont.start();
  }

  /**
   * Map the dynamic port to Spring Boot's datasource.
   * @param registry Registry.
   */
  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgresCont::getJdbcUrl);
    registry.add("spring.datasource.username", postgresCont::getUsername);
    registry.add("spring.datasource.password", postgresCont::getPassword);
  }

  /**
   * Reset basic things.
   */
  @AfterEach
  protected void resetBaseIntegration() {
    clock.reset();
  }

  // //////////////////////////////////////////////////////////////////////////

  /**
   * Reset state of database so tests don't interfere with each other.
   */
  protected void resetDatabase() {
    cleanDatabase();
    setupDatabase();
  }

  /**
   * Clean up the database. Inherited methods need to call <code>super.cleanDatabase()</code> at beginning.
   */
  protected void cleanDatabase() {
    configRepository.deleteAll();
    systemHistoryRepository.deleteAll();
  }

  /**
   * Set up the database. Inherited methods need to call <code>super.setupDatabase()</code> at beginning.
   */
  protected void setupDatabase() {
    Config config = new Config();
    config.setName(ConfigConst.USER_LOCKDOWN);
    config.setValue(ConfigConst.USER_LOCKDOWN_DEF);
    config.setDescription("-");
    configRepository.save(config);
  }
}
