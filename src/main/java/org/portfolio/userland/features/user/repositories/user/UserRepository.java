package org.portfolio.userland.features.user.repositories.user;

import org.portfolio.userland.features.user.entities.User;
import org.springframework.data.jpa.repository.EntityGraph;
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
public interface UserRepository extends JpaRepository<User, Long>, UserCustomRepository {
  /**
   * Check if user with that email already exists.
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
   * Find user by email for authorization purposes. Eagerly loads permissions, as these are always needed during authorization.
   * @param email Email.
   * @return User or empty optional.
   */
  @EntityGraph(attributePaths = {
      "permissions",
      "permissions.permission"
  })
  Optional<User> findAuthByEmail(String email);

  //

  /**
   * Find user by id for authorization purposes. Eagerly loads permissions, as these are always needed during
   * authorization (e.g. rebuilding <code>perms</code> claim on session prolongation).
   * @param id User identifier.
   * @return User or empty optional.
   */
  @EntityGraph(attributePaths = {
      "permissions",
      "permissions.permission"
  })
  Optional<User> findAuthById(Long id);

  //

  /**
   * Find only authorization state of user (id, status, locked) by email and JWT token in one query. Deliberately
   * avoids loading permissions and other data, as <code>JwtAuthFilter</code> needs just the state on every request -
   * permissions are taken from signed JWT claims. The join with the JWT table simultaneously serves as the revocation
   * check: a revoked (deleted) token yields no row, so the whole per-request authentication costs a single indexed
   * SELECT instead of two separate queries.
   * @param email Email from verified JWT subject claim.
   * @param token Raw JWT string.
   * @return Authorization state or empty optional (unknown user or revoked/unknown token).
   */
  @Query("SELECT new org.portfolio.userland.features.user.repositories.user.UserAuthState(u.id, u.status, u.locked) "
      + "FROM UserJwt j JOIN j.user u WHERE u.email = :email AND j.token = :token")
  Optional<UserAuthState> findAuthStateByEmailAndToken(@Param("email") String email, @Param("token") String token);

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
