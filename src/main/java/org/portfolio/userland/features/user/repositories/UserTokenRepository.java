package org.portfolio.userland.features.user.repositories;

import org.portfolio.userland.features.user.entities.EnTokenType;
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

  /**
   * Checks if given token type for given user exists.
   * @param userId Identificator of user.
   * @param type Type of token.
   * @return True if user token of this type exists, otherwise false.
   */
  @Query("SELECT t FROM UserToken t WHERE t.user.id = :userId and t.type = :type")
  Optional<UserToken> findByUserAndType(Long userId, EnTokenType type);

  //

  /**
   * Delete all expired tokens.
   * @param nowAt Current date and time.
   * @return Count of removed tokens.
   */
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("DELETE FROM UserToken t WHERE t.expiresAt < :nowAt")
  int deleteExpiredTokens(@Param("nowAt") LocalDateTime nowAt);

  /**
   * Delete given token.
   * @param token Token string.
   * @return Count of removed tokens.
   */
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("DELETE FROM UserToken t WHERE t.token = :token")
  int deleteToken(@Param("token") String token);
}
