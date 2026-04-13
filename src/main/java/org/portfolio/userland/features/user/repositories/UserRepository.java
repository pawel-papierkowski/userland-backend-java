package org.portfolio.userland.features.user.repositories;

import org.portfolio.userland.features.user.data.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Database interface for user.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
  /**
   * Helpful for registration validation.
   * @param email Email.
   * @return True if user with that email already exists, otherwise false.
   */
  boolean existsByEmail(String email);

  /**
   * Find user by email.
   * @param email Email.
   * @return User or empty optional.
   */
  Optional<User> findByEmail(String email);

  //

  /**
   * Delete all pending users that are too old.
   * @param cutoffDateAt Cutoff date.
   * @return Count of removed users.
   */
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("DELETE FROM User u WHERE u.status = 'PENDING' AND u.createdAt < :cutoffDateAt")
  int deletePendingUsersOlderThan(@Param("cutoffDateAt") LocalDateTime cutoffDateAt);
}
