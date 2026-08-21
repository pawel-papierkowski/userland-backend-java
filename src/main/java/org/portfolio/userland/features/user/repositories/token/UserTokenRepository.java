package org.portfolio.userland.features.user.repositories.token;

import org.portfolio.userland.features.user.entities.EnUserTokenType;
import org.portfolio.userland.features.user.entities.UserToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Database interface for user token entry.
 */
@Repository
public interface UserTokenRepository extends JpaRepository<UserToken, Long>, UserTokenCustomRepository {
  /**
   * Just checks if given token string exists.
   * @param type Type of token.
   * @param token Token string.
   * @return True if user token exists, otherwise false.
   */
  boolean existsByTypeAndToken(EnUserTokenType type, String token);

  /**
   * Find token by type and token string.
   * @param type Type of token.
   * @param token Token string.
   * @return User token or empty optional.
   */
  Optional<UserToken> findByTypeAndToken(EnUserTokenType type, String token);

  /**
   * Checks if given token type for given user exists.
   * @param userId Identificator of user.
   * @param type Type of token.
   * @return True if user token of this type exists, otherwise false.
   */
  @Query("SELECT t FROM UserToken t WHERE t.user.id = :userId and t.type = :type")
  Optional<UserToken> findByUserAndType(@Param("userId") Long userId, @Param("type") EnUserTokenType type);

  /**
   * Atomically consumes given token: deletes it only if it exists and is not expired yet.
   * <p>Note: this is a compare-and-delete operation, safe to be called by multiple concurrent transactions -
   * exactly one of them will get return value of 1. Intended to be used right after {@link #findByTypeAndToken}.</p>
   * @param type Type of token.
   * @param token Token string.
   * @param nowAt Current date&time.
   * @return Count of removed tokens (1 if token was consumed, 0 if it was already consumed or expired).
   */
  @Modifying
  @Query("DELETE FROM UserToken t WHERE t.type = :type AND t.token = :token AND t.expiresAt > :nowAt")
  int consumeToken(@Param("type") EnUserTokenType type, @Param("token") String token, @Param("nowAt") LocalDateTime nowAt);

  //

  /**
   * Delete all expired tokens.
   * @param nowAt Current date and time.
   * @return Count of removed tokens.
   */
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("DELETE FROM UserToken t WHERE t.expiresAt < :nowAt")
  int deleteExpiredTokens(@Param("nowAt") LocalDateTime nowAt);
}
