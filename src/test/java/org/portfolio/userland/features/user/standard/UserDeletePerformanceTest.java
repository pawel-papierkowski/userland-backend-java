package org.portfolio.userland.features.user.standard;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.portfolio.userland.features.user.BaseUserTest;
import org.portfolio.userland.features.user.dto.standard.delete.UserDeleteConfirmReq;
import org.portfolio.userland.features.user.entities.*;
import org.portfolio.userland.features.user.services.standard.UserDeleteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests that deleting a user does not scale database queries with the size of his collections.
 * <p>User account has 5 collections (configs, history, tokens, jwts, permissions) that are all
 * {@code @OneToMany(cascade = ALL, orphanRemoval = true)}. On {@code userRepository.delete(user)} Hibernate
 * initializes every lazy collection and issues one DELETE per child row, even though the database already has
 * {@code ON DELETE CASCADE} on all related foreign keys. For a user with large history this becomes N+1.</p>
 */
public class UserDeletePerformanceTest extends BaseUserTest {
  @Autowired
  private UserDeleteService userDeleteService;

  @BeforeEach
  public void tearDown() {
    resetDatabase();
  }

  // //////////////////////////////////////////////////////////////////////////

  /**
   * Deleting user must not issue queries proportional to size of user collections.
   * <p>Expected: ~3 statements (resolve token + load user + single DELETE with DB cascade),
   * no lazy collection loads, single entity delete.</p>
   */
  @Test
  @Transactional
  public void deleteUserQueriesDoNotScaleWithCollectionSize() {
    clock.setFixedTime("2026-04-10T10:00:00Z");

    // Arrange: Create active user in database state indicating it requested account deletion.
    // Note history has potential to be large, hence we add plenty of events.
    User user = userFactory.genUser(EnUserStatus.ACTIVE);
    for (int i = 0; i < 100; i++) {
      userHistoryFactory.genHistoryEvent(user, EnUserHistoryWho.USER, EnUserHistoryWhat.LOGIN, "");
    }
    userConfigFactory.genConfig(user, "some.config.name", "some.config.value");
    userJwtFactory.genJwtEntry(user, "FAKE_JWT");
    userPermissionFactory.genPermissionEntry(user, permissionRepository.findByName("role").orElseThrow(), "operator");
    UserToken token = userTokenFactory.genTokenEntry(user, EnUserTokenType.DELETE, null);
    userRepository.save(user);
    entityManager.flush();
    // Detach everything, so delete flow re-loads user with lazy, uninitialized collections (like production does).
    entityManager.clear();

    // Prepare query counting.
    @SuppressWarnings("resource") // Note: SessionFactory is AutoCloseable, but we must NOT close it - it is the shared application SessionFactory. Unwrap is safe here.
    Statistics statistics = entityManager.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
    assertThat(statistics.isStatisticsEnabled()).as("Hibernate statistics are not enabled").isTrue();
    long stmtsBefore = statistics.getPrepareStatementCount();
    long collectionLoadsBefore = statistics.getCollectionLoadCount();
    long entityDeletesBefore = statistics.getEntityDeleteCount();

    // Act: Delete user account.
    userDeleteService.delete(new UserDeleteConfirmReq(token.getToken()));
    entityManager.flush();

    // Assert: Query count must be constant, not dependent on size of collections.
    assertThat(statistics.getPrepareStatementCount() - stmtsBefore)
        .as("Deleting user should not issue per-child SELECT/DELETE statements")
        .isLessThanOrEqualTo(5);
    assertThat(statistics.getCollectionLoadCount() - collectionLoadsBefore)
        .as("Deleting user should not initialize any lazy collection")
        .isZero();
    assertThat(statistics.getEntityDeleteCount() - entityDeletesBefore)
        .as("Deleting user should delete only the user entity, rest is handled by DB cascade")
        .isLessThanOrEqualTo(1);
  }
}