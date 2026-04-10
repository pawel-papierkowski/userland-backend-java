package org.portfolio.userland.base;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.portfolio.userland.features.user.controllers.UserController;
import org.portfolio.userland.utils.problemDetail.ProblemDetailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Base class for all pure web API tests.
 * Note: security is processed differently than in full-fledged Spring Boot tests. To avoid issues, we completely
 * disable Spring Security.
 */
@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false) // disable web security
@Import(ProblemDetailService.class) // because WebMvcTest by default ignores services
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
