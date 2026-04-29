package org.portfolio.userland.test.helpers.context;

import org.portfolio.userland.system.auth.details.CustomUserDetails;
import org.springframework.security.test.context.support.WithSecurityContext;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mocks logged in <code>CustomUserDetails</code>.
 * Note that even with this you will often need to create users in database.
 * @see CustomUserDetails Custom user details.
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@WithSecurityContext(factory = WithMockCustomUserSecurityContextFactory.class)
public @interface WithMockCustomUser {
  long id() default 1L;
  boolean active() default true;
  boolean locked() default false;
  String username() default "test-user";
  String email() default "test@example.com";
  String password() default "$2a$10$testtesttesttesttestte";
  String[] jwts() default { "TOKEN" };
  String[] authorities() default { "ROLE_USER" };
}
