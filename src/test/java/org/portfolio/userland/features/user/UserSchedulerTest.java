package org.portfolio.userland.features.user;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.portfolio.userland.features.user.entities.EnUserStatus;
import org.portfolio.userland.features.user.entities.User;
import org.portfolio.userland.features.user.entities.UserJwt;
import org.portfolio.userland.features.user.entities.UserToken;
import org.portfolio.userland.features.user.scheduler.UserScheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testing code that is called via user scheduler.
 */
public class UserSchedulerTest extends BaseUserTest {
  @Autowired
  private UserScheduler userScheduler;

  @AfterEach
  public void tearDown() {
    // Clean up the database after every test so tests don't interfere with each other.
    userRepository.deleteAll();
  }

  // //////////////////////////////////////////////////////////////////////////

  @Test
  @Transactional
  public void cleanPendingUsers() {
    // Arrange: add a bunch of random users ensuring each one has different creation time.
    clock.setFixedTime("2026-04-10T10:00:00Z");
    userRepository.save(userFactory.genRandUser(EnUserStatus.PENDING)); // this user will be removed
    User u2 = userRepository.save(userFactory.genRandUser(EnUserStatus.ACTIVE)); // this user will NOT be removed because it is not PENDING
    clock.setFixedTime("2026-04-10T22:00:00Z");
    User u3 = userRepository.save(userFactory.genRandUser(EnUserStatus.PENDING));
    clock.setFixedTime("2026-04-11T10:00:00Z");
    User u4 = userRepository.save(userFactory.genRandUser(EnUserStatus.PENDING));

    entityManager.flush();
    entityManager.clear();

    // Act: manually call scheduler method for cleaning up old pending users.
    clock.setFixedTime("2026-04-12T12:30:00Z");
    userScheduler.cleanExpiredUsers();

    // Assert: only some users should exist, rest is deleted.
    assertThat(userRepository.count()).as("Count of all users is wrong").isEqualTo(3);
    // Make sure correct users survived.
    List<User> users = userRepository.findAll();
    assertThat(users.contains(u2)).as("User 2 (ACTIVE, never cleaned) should exist").isEqualTo(true);
    assertThat(users.contains(u3)).as("User 3 (PENDING, but too young) should exist").isEqualTo(true);
    assertThat(users.contains(u4)).as("User 4 (PENDING, but too young) should exist").isEqualTo(true);
  }

  @Test
  @Transactional
  public void cleanExpiredTokens() {
    // Arrange: add a bunch of random users with tokens ensuring each one has different expiration time.
    // Test will remove only some of them.
    clock.setFixedTime("2026-04-10T10:00:00Z");
    userRepository.save(userFactory.genRandUser(EnUserStatus.PENDING));
    clock.setFixedTime("2026-04-10T22:00:00Z");
    userRepository.save(userFactory.genRandUser(EnUserStatus.PENDING));
    clock.setFixedTime("2026-04-11T10:00:00Z");
    User u3 = userRepository.save(userFactory.genRandUser(EnUserStatus.PENDING)); // this user will have surviving token

    entityManager.flush();
    entityManager.clear();

    // Act: manually call scheduler method for cleaning up expired tokens.
    clock.setFixedTime("2026-04-11T22:30:00Z");
    userScheduler.cleanExpiredTokens();

    // Assert: only one token should exist, rest is deleted.
    assertThat(userTokenRepository.count()).as("Count of all user tokens is wrong").isEqualTo(1);
    // Make sure correct token survived.
    UserToken userToken = userTokenRepository.findAll().getFirst();
    assertThat(userToken.getUser().getId()).as("Wrong user id for token").isEqualTo(u3.getId());
  }

  @Test
  @Transactional
  public void cleanExpiredJwts() {
    // Arrange: add a bunch of random users with JWT entries ensuring each one has different expiration time.
    // Test will remove only some of them.
    clock.setFixedTime("2026-04-10T10:00:00Z");
    User u1 = userFactory.genRandUser(EnUserStatus.ACTIVE);
    userJwtFactory.genJwtEntry(u1, "JWT token 1");
    userRepository.save(u1);
    clock.setFixedTime("2026-04-10T22:00:00Z");
    User u2 = userFactory.genRandUser(EnUserStatus.ACTIVE);
    userJwtFactory.genJwtEntry(u2, "JWT token 2");
    userRepository.save(u2);
    clock.setFixedTime("2026-04-11T10:00:00Z");
    User u3 = userFactory.genRandUser(EnUserStatus.ACTIVE);
    userJwtFactory.genJwtEntry(u3, "JWT token 3");
    userRepository.save(u3); // this user will have surviving JWT

    entityManager.flush();
    entityManager.clear();

    // Act: manually call scheduler method for cleaning up expired tokens.
    clock.setFixedTime("2026-04-11T12:30:00Z");
    userScheduler.cleanExpiredJwts();

    // Assert: only one token should exist, rest is deleted.
    assertThat(userJwtRepository.count()).as("Count of all user JWTs is wrong").isEqualTo(1);
    // Make sure correct token survived.
    UserJwt userJwt = userJwtRepository.findAll().getFirst();
    assertThat(userJwt.getUser().getId()).as("Wrong user id for JWT").isEqualTo(u3.getId());
  }
}
