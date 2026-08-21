package org.portfolio.userland.features.user.admin;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.portfolio.userland.common.dto.TableMetaReq;
import org.portfolio.userland.common.services.table.TableHelper;
import org.portfolio.userland.features.user.BaseUserTest;
import org.portfolio.userland.features.user.dto.admin.permission.UserPermissionTableReq;
import org.portfolio.userland.features.user.entities.EnUserStatus;
import org.portfolio.userland.features.user.entities.Permission;
import org.portfolio.userland.features.user.entities.User;
import org.portfolio.userland.features.user.entities.UserPermission;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests that fetching user permissions page does not cause N+1 queries when mapping entities to DTOs.
 * <p>The mapper accesses {@code userPermission.getPermission().getName()} which traverses a
 * {@code @ManyToOne(fetch = LAZY)} association. Without a fetch join, each entity in the result
 * page would trigger a separate SQL query to load the {@code Permission} entity — classic N+1.</p>
 * <p>Note: each entry uses a <em>distinct</em> permission, so persistence context deduplication
 * cannot hide the extra queries. Also {@code default_batch_fetch_size} is disabled for tests,
 * so lazy loads cannot be batched away either.</p>
 */
public class UserPermissionPerformanceTest extends BaseUserTest {
  @BeforeEach
  public void tearDown() {
    resetDatabase();
  }

  // //////////////////////////////////////////////////////////////////////////

  /**
   * Fetching a page of user permissions must use a constant number of SQL statements,
   * regardless of how many permission entries the user has.
   * <p>Expected: exactly 1 SELECT (with LEFT JOIN fetch) — viewPage() itself issues no COUNT query.
   * Zero collection loads, zero extra entity queries.</p>
   */
  @Test
  @Transactional
  public void viewPageQueriesDoNotScaleWithEntryCount() {
    // Arrange: Create user with many permission entries, each referencing a DISTINCT permission.
    // Distinct permissions are important: reusing the same permission would let the persistence
    // context serve subsequent lazy loads without SQL, masking the N+1 problem.
    User user = userFactory.genUser(EnUserStatus.ACTIVE);
    int entryCount = 50;
    for (int i = 0; i < entryCount; i++) {
      Permission permission = new Permission();
      permission.setName("perm_" + i);
      permission.setInJwt(false);
      permission.setInAuthorities(false);
      permission = permissionRepository.save(permission);
      userPermissionFactory.genPermissionEntry(user, permission, "value_" + i);
    }
    userRepository.save(user);
    entityManager.flush();
    // Detach everything, so viewPage re-loads entities with lazy, uninitialized associations (like production does).
    entityManager.clear();

    // Prepare query counting.
    @SuppressWarnings("resource")
    Statistics statistics = entityManager.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
    assertThat(statistics.isStatisticsEnabled()).as("Hibernate statistics are not enabled").isTrue();
    long stmtsBefore = statistics.getPrepareStatementCount();
    long collectionLoadsBefore = statistics.getCollectionLoadCount();

    // Act: Load page of user permissions — same path as UserPermissionTableService.getPage().
    UserPermissionTableReq tableReq = UserPermissionTableReq.builder()
        .userId(user.getId())
        .tableMeta(TableMetaReq.builder()
            .pageSize(entryCount) // large enough to return all entries
            .page(0)
            .sortBy("createdAt")
            .build())
        .build();

    tableReq = tableReq.toBuilder()
        .tableMeta(TableHelper.prepareTableMeta(tableReq.tableMeta()))
        .build();

    List<UserPermission> page = userPermissionRepository.viewPage(tableReq);

    // Trigger the mapper that was causing N+1 (accesses userPermission.getPermission().getName()).
    for (UserPermission entity : page) {
      userMapper.entityToTableEntry(entity);
    }

    // Assert: Query count must be constant, not dependent on number of permission entries.
    long stmtsDelta = statistics.getPrepareStatementCount() - stmtsBefore;
    long collectionLoadsDelta = statistics.getCollectionLoadCount() - collectionLoadsBefore;

    assertThat(stmtsDelta)
        .as("Loading %d permission entries should use exactly 1 query (SELECT with fetch join), not N+1", entryCount)
        .isEqualTo(1); // 1 SELECT with LEFT JOIN — viewPage() issues no COUNT query
    assertThat(collectionLoadsDelta)
        .as("Should not initialize any lazy collection")
        .isZero();
  }
}
