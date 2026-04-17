package org.portfolio.userland.test.base;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.portfolio.userland.test.helpers.problemDetail.ProblemDetailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Base class for all pure web API tests.
 */
@WebTest
public abstract class BaseWebTest {
  /** Used to simulate HTTP requests. */
  @Autowired
  protected MockMvc mockMvc;

  /** Service to assert Problem Detail. */
  @Autowired
  protected ProblemDetailService problemDetailService;

  /** Used to convert Java objects to JSON. */
  protected final ObjectMapper objectMapper = new ObjectMapper();
}
