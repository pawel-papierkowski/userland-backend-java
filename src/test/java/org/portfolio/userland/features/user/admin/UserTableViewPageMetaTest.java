package org.portfolio.userland.features.user.admin;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.portfolio.userland.common.dto.TableMetaReq;
import org.portfolio.userland.features.user.BaseUserTest;
import org.portfolio.userland.features.user.dto.admin.user.UserTableReq;
import org.portfolio.userland.features.user.entities.EnUserStatus;
import org.portfolio.userland.features.user.entities.User;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression tests for null/partially filled <code>tableMeta</code> handling in
 * {@link org.portfolio.userland.common.repositories.EntityTableHandling#viewPage}.
 * <p>Before normalization was moved into <code>viewPage()</code> itself, calling it with a null or partially filled
 * <code>tableMeta</code> crashed with NPE inside sorting (<code>sortBy</code> was dereferenced without defaults),
 * even though pagination was already null-safe.</p>
 */
public class UserTableViewPageMetaTest extends BaseUserTest {
  @BeforeEach
  public void setup() {
    resetDatabase();
  }

  // //////////////////////////////////////////////////////////////////////////

  /**
   * <code>tableMeta</code> completely missing must fall back to defaults (page size 20, page 0, sort by createdAt DESC)
   * instead of throwing NPE.
   */
  @Test
  @Transactional
  public void viewPageWithNullTableMetaAppliesDefaults() {
    // Arrange: three users, remember insertion order of ids.
    List<Long> savedIds = new ArrayList<>();
    for (int i = 0; i < 3; i++) {
      User user = userRepository.save(userFactory.genRandUser(EnUserStatus.ACTIVE));
      savedIds.add(user.getId());
    }
    entityManager.flush();

    // Act: no table metadata at all.
    UserTableReq req = UserTableReq.builder()
        .tableMeta(null)
        .build();

    List<User> page = userRepository.viewPage(req);

    // Assert: all users returned, default sorting (createdAt DESC with id DESC fallback) puts newest first,
    // which for identical creation timestamps means reverse insertion order.
    assertThat(page).hasSize(savedIds.size());
    assertThat(page).extracting(User::getId).containsExactly(savedIds.get(2), savedIds.get(1), savedIds.get(0));
  }

  /**
   * Partially filled <code>tableMeta</code> (only pageSize set, no sorting data) must fill missing fields with
   * defaults instead of throwing NPE on null <code>sortBy</code>.
   */
  @Test
  @Transactional
  public void viewPageWithPartiallyFilledTableMetaAppliesDefaults() {
    // Arrange: three users, but page size limited to two.
    for (int i = 0; i < 3; i++) {
      userRepository.save(userFactory.genRandUser(EnUserStatus.ACTIVE));
    }
    entityManager.flush();

    // Act: only pageSize provided - sortBy/sortOrder/page are left null.
    UserTableReq req = UserTableReq.builder()
        .tableMeta(TableMetaReq.builder()
            .pageSize(2)
            .build())
        .build();

    List<User> page = userRepository.viewPage(req);

    // Assert: page limit respected, no exception despite missing sort field.
    assertThat(page).hasSize(2);
  }
}
