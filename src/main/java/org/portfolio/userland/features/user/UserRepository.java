package org.portfolio.userland.features.user;

import org.portfolio.userland.features.user.data.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Database interface for user.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

  /**
   * Find user by email.
   * @param email Email.
   * @return User or empty optional.
   */
  Optional<User> findByEmail(String email);

  /**
   * Helpful for registration validation.
   * @param email Email.
   * @return True if user with that email already exists, otherwise false.
   */
  boolean existsByEmail(String email);
}
