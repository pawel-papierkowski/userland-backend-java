package org.portfolio.userland.system.auth.annotations;

import org.springframework.security.access.prepost.PreAuthorize;
import java.lang.annotation.*;

/**
 * Security annotation for administrative-only actions.
 * Grants access strictly to ROLE_ADMIN. ROLE_OPERATOR is denied even with specific authorities.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@PreAuthorize("hasRole('ADMIN')")
public @interface HasAdminPermission {
}