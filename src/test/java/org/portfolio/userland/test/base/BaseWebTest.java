package org.portfolio.userland.test.base;

import org.portfolio.userland.test.helpers.problemDetail.ProblemDetailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

/**
 * Base class for all pure web API tests.
 */
public abstract class BaseWebTest {
  /** Used to simulate HTTP requests. */
  @Autowired
  protected MockMvc mockMvc;

  /** Service to assert Problem Detail. */
  @Autowired
  protected ProblemDetailService problemDetailService;

  /**
   * Spring's auto-configured ObjectMapper (Jackson 3), used to convert Java objects to JSON for request bodies.
   */
  @Autowired
  protected ObjectMapper objectMapper;
}
