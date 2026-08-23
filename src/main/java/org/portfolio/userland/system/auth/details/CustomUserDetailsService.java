package org.portfolio.userland.system.auth.details;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.portfolio.userland.features.user.entities.EnUserStatus;
import org.portfolio.userland.features.user.repositories.user.UserRepository;
import org.portfolio.userland.system.auth.jwt.constants.JwtClaims;
import org.portfolio.userland.system.auth.perm.PermissionHelper;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Builds <code>CustomUserDetails</code> for <code>JwtAuthFilter</code>.
 * <p>Note: authorities (permissions) are taken from signed JWT <code>perms</code> claim instead of database.
 * This is safe because:</p>
 * <ul>
 *   <li>The claim is cryptographically signed - client cannot modify it without invalidating signature.</li>
 *   <li>Instant permission changes are guaranteed by deleting all JWT entries of given user
 *       (see <code>UserPermissionTableService</code>) - old tokens die immediately and re-login issues a token
 *       with fresh permissions.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService {
  private final UserRepository userRepository;

  /**
   * Locates user data based on verified JWT claims. Permissions are resolved from token claims (signed), user state
   * (id/status/locked) is always loaded fresh from database so that locking/disabling takes effect immediately.
   * The same query also verifies the token is not revoked, keeping per-request authentication at a single
   * indexed SELECT (unknown user or revoked token results in empty database row).
   * @param claims Verified JWT claims (signature and expiration already validated by parser).
   * @param jwtStr Raw JWT string for the revocation check.
   * @return User details or null if subject/email is missing or user/token pair does not exist in database.
   */
  public @Nullable CustomUserDetails loadFromToken(@NonNull Claims claims, @NonNull String jwtStr) {
    // Note we use special slim query that loads just authorization state and simultaneously checks that the JWT
    // entry exists (not revoked), as permissions come from token claims.
    return userRepository.findAuthStateByEmailAndToken(claims.getSubject(), jwtStr)
        .map(state -> new CustomUserDetails(
            state.id(),
            EnUserStatus.ACTIVE.equals(state.status()),
            Boolean.TRUE.equals(state.locked()),
            resolveUsername(claims),
            claims.getSubject(),
            resolveAuthorities(claims)))
        .orElse(null);
  }

  /**
   * Resolves username from <code>name</code> claim. Falls back to subject (email) if claim is missing,
   * as {@link CustomUserDetails#getUsername()} must never return null.
   * @param claims Verified JWT claims.
   * @return Username or email.
   */
  private static @NonNull String resolveUsername(@NonNull Claims claims) {
    String name = claims.get(JwtClaims.NAME, String.class);
    return name != null ? name : claims.getSubject();
  }

  /**
   * Resolves authorities from signed <code>perms</code> claim. Missing claim results in empty authorities.
   * @param claims Verified JWT claims.
   * @return Authorities.
   */
  private static Collection<? extends GrantedAuthority> resolveAuthorities(@NonNull Claims claims) {
    Object permsClaim = claims.get(JwtClaims.PERMS);
    if (!(permsClaim instanceof Map<?, ?> permsMap)) return List.of();
    return PermissionHelper.resolveAuthoritiesFromClaim(permsMap);
  }
}
