package org.portfolio.userland.features.user.repositories;

import org.portfolio.userland.features.user.data.EnTokenType;
import org.portfolio.userland.features.user.data.UserToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Database interface for user token entry.
 */
@Repository
public interface UserTokenRepository extends JpaRepository<UserToken, Long> {
  /**
   * Just checks if given token string exists.
   * @param type Type of token.
   * @param token Token string.
   * @return True if user token exists, otherwise false.
   */
  boolean existsByTypeAndToken(EnTokenType type, String token);

  /**
   * Find token by type and token string.
   * @param type Type of token.
   * @param token Token string.
   * @return User token or empty optional.
   */
  Optional<UserToken> findByTypeAndToken(EnTokenType type, String token);
}
