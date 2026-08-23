package org.portfolio.userland.features.user.repositories.user;

import org.portfolio.userland.features.user.entities.EnUserStatus;

/**
 * Slim view of user authorization state. Used by <code>JwtAuthFilter</code> to verify user state on every request
 * without loading the whole <code>User</code> entity (permissions come from JWT claims, so they are not needed here).
 * @param id User identificator.
 * @param status Status of user.
 * @param locked Is user locked?
 */
public record UserAuthState(Long id, EnUserStatus status, Boolean locked) {
}
