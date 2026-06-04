package org.portfolio.userland.system.auth.annotations;

import org.springframework.security.access.prepost.PreAuthorize;
import java.lang.annotation.*;

/**
 * Security annotation for viewing user-related data.
 * Grants access to ROLE_ADMIN or ROLE_OPERATOR with the USER_VIEW authority.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@PreAuthorize("hasRole('ADMIN') or (hasRole('OPERATOR') and hasAuthority('USER_VIEW'))")
public @interface HasUserViewPermission {
}