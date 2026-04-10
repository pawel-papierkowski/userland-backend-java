package org.portfolio.userland.features.user.repositories;

import org.portfolio.userland.features.user.data.UserToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Database interface for user token.
 */
@Repository
public interface UserTokenRepository extends JpaRepository<UserToken, Long> {
  /**
   * Just checks if given token string exists.
   * @param token Token string.
   * @return True if user token exists, otherwise false.
   */
  boolean existsByToken(String token);

  /**
   * Find token by token string.
   * @param token Token string.
   * @return User token or empty optional.
   */
  Optional<UserToken> findByToken(String token);
}
