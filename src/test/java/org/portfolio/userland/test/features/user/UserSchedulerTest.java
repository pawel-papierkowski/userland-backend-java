package org.portfolio.userland.test.features.user;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.portfolio.userland.base.BaseIntegrationTest;
import org.portfolio.userland.features.user.data.EnUserStatus;
import org.portfolio.userland.features.user.data.User;
import org.portfolio.userland.features.user.data.UserToken;
import org.portfolio.userland.features.user.repositories.UserRepository;
import org.portfolio.userland.features.user.repositories.UserTokenRepository;
import org.portfolio.userland.features.user.scheduler.UserScheduler;
import org.portfolio.userland.helpers.factories.UserFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testing code that is called via user scheduler.
 */
public class UserSchedulerTest extends BaseIntegrationTest {
  @Autowired
  private UserRepository userRepository;
  @Autowired
  private UserTokenRepository userTokenRepository;

  @Autowired
  private UserFactory userFactory;

  @Autowired
  private UserScheduler userScheduler;

  @AfterEach
  void tearDown() {
    // Clean up the database after every test so tests don't interfere with each other.
    userTokenRepository.deleteAll();
    userRepository.deleteAll();
  }

  // //////////////////////////////////////////////////////////////////////////

  @Test
  @Transactional
  void cleanPendingUsers() throws Exception {
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
    userScheduler.cleanPendingUsers();

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
  void cleanExpiredTokens() throws Exception {
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
}
