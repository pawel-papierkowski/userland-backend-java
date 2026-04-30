package org.portfolio.userland.features.user.repositories;

import org.portfolio.userland.features.user.entities.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Database interface for user profile.
 */
@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {
}
