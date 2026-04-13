package org.portfolio.userland.base;

import org.springframework.test.context.ActiveProfiles;

import java.lang.annotation.*;

/**
 * Meta-annotation for any kind of test.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@ActiveProfiles("test") // all tests use this profile
public @interface AnyTest {
}
