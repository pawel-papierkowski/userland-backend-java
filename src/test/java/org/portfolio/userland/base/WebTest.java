package org.portfolio.userland.base;

import org.portfolio.userland.features.user.controllers.UserController;
import org.portfolio.userland.helpers.problemDetail.ProblemDetailService;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * Base annotation for web tests.
 * Note: security is processed differently than in full-fledged Spring Boot tests. To avoid issues, we completely
 * disable Spring Security. Security will be tested separately.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@AnyTest
@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false) // disable web security
@Import(ProblemDetailService.class) // because WebMvcTest by default ignores services
public @interface WebTest {
}
