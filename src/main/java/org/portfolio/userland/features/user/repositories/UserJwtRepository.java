package org.portfolio.userland.features.user.repositories;

import org.portfolio.userland.features.user.entities.UserJwt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

/**
 * Database interface for user JWT entry.
 */
@Repository
public interface UserJwtRepository extends JpaRepository<UserJwt, Long>, UserJwtCustomRepository {

  /**
   * Delete all expired JWTs.
   * @param nowAt Current date and time.
   * @return Count of removed JWTs.
   */
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("DELETE FROM UserJwt t WHERE t.expiresAt < :nowAt")
  int deleteExpiredJwts(@Param("nowAt") LocalDateTime nowAt);

  /**
   * Delete all JWTs for given user.
   * @param userId User identificator.
   * @return Count of removed JWTs.
   */
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("DELETE FROM UserJwt t WHERE t.user.id = :userId")
  int deleteAllByUser(@Param("userId") Long userId);
}
