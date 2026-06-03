package org.portfolio.userland.features.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.portfolio.userland.features.user.entities.*;
import org.portfolio.userland.features.user.schedulers.UserScheduler;
import org.portfolio.userland.system.config.service.ConfigConst;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testing user maintenance service.
 */
public class UserMaintenanceTest extends BaseUserTest {
  @Autowired
  private UserScheduler userScheduler;

  @BeforeEach
  public void tearDown() {
    resetDatabase();
  }

  // //////////////////////////////////////////////////////////////////////////

  @Test
  @Transactional
  public void cleanPendingUsers() {
    // Arrange: add a bunch of random users ensuring each one has different creation time.
    clock.setFixedTime("2026-04-10T10:00:00Z");
    userRepository.save(userFactory.genRandUser(EnUserStatus.PENDING)); // this user will be removed, it is PENDING and old enough
    User u2 = userRepository.save(userFactory.genRandUser(EnUserStatus.ACTIVE)); // this user will NOT be removed because it is not PENDING
    clock.setFixedTime("2026-04-10T22:00:00Z");
    User u3 = userRepository.save(userFactory.genRandUser(EnUserStatus.PENDING));

    entityManager.flush();
    entityManager.clear();

    // Act: manually call scheduler method for cleaning up old pending users.
    clock.setFixedTime("2026-04-12T12:30:00Z"); // anything before "2026-04-10T12:30:00Z" will be behind cutoff
    userScheduler.cleanPendingUsers();

    // Assert: only some users should exist, rest is deleted.
    assertThat(userRepository.count()).as("Count of all users is wrong").isEqualTo(2);
    // Make sure correct users survived.
    List<User> users = userRepository.findAll();
    assertThat(users.contains(u2)).as("User 2 (old enough but ACTIVE) should exist").isEqualTo(true);
    assertThat(users.contains(u3)).as("User 3 (PENDING, but too young) should exist").isEqualTo(true);
  }


  @Test
  @Transactional
  public void cleanActiveUsers() {
    // Not in portfolio mode, no users will be removed.
    cleanActiveUsersAA();

    // Assert: all users should exist.
    assertThat(userRepository.count()).as("Count of all users is wrong").isEqualTo(3);
  }

  @Test
  @Transactional
  public void cleanActiveUsersPortfolio() {
    configService.set(ConfigConst.GENERAL_PORTFOLIO, "1"); // set to portfolio mode
    User[] savedUsers = cleanActiveUsersAA();

    // Assert: only some users should exist, rest is deleted.
    assertThat(userRepository.count()).as("Count of all users is wrong").isEqualTo(2);
    // Make sure correct users survived.
    List<User> users = userRepository.findAll();
    assertThat(users.contains(savedUsers[0])).as("User 0 (ACTIVE but recently active) should exist").isEqualTo(true);
    assertThat(users.contains(savedUsers[2])).as("User 2 (idle for long but PENDING) should exist").isEqualTo(true);
  }

  /**
   * Clean active users: arrange and act.
   * @return Users created during arrange.
   */
  private User[] cleanActiveUsersAA() {
    // Arrange: add a bunch of random users ensuring each one has different creation time.
    clock.setFixedTime("2026-04-10T10:00:00Z");
    User u0 = userRepository.save(userFactory.genRandUser(EnUserStatus.ACTIVE)); // Will not be removed due to recent activity.
    User u1 = userRepository.save(userFactory.genRandUser(EnUserStatus.ACTIVE)); // Will be removed due to being idle for too long.
    User u2 = userRepository.save(userFactory.genRandUser(EnUserStatus.PENDING)); // Will not be removed due to PENDING status, even though it is idle for too long.

    // Arrange: add some entries in history at certain times.
    clock.setFixedTime("2026-04-20T14:00:00Z");
    userHistoryFactory.genHistoryEvent(u0, EnUserHistoryWho.USER, EnUserHistoryWhat.LOGIN, "");
    clock.setFixedTime("2026-04-20T10:00:00Z");
    userHistoryFactory.genHistoryEvent(u1, EnUserHistoryWho.USER, EnUserHistoryWhat.LOGIN, "");

    entityManager.flush();
    entityManager.clear();

    // Act: manually call scheduler method for cleaning up old active users.
    clock.setFixedTime("2026-04-23T12:30:00Z"); // anything before "2026-04-20T12:30:00Z" will be behind cutoff
    userScheduler.cleanActiveUsers();

    return new User[] {u0, u1, u2};
  }

  //

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

  //

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
