package org.portfolio.userland.system.history;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.portfolio.userland.features.user.entities.EnUserStatus;
import org.portfolio.userland.features.user.entities.User;
import org.portfolio.userland.system.BaseSystemTest;
import org.portfolio.userland.system.history.entities.EnHistoryWhat;
import org.portfolio.userland.system.history.entities.EnHistoryWho;
import org.portfolio.userland.system.history.entities.SystemHistory;
import org.portfolio.userland.system.history.services.SystemHistoryService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * Tests <code>SystemHistoryService</code>.
 */
public class SystemHistoryServiceTest extends BaseSystemTest {
  @Autowired
  private SystemHistoryService systemHistoryService;

  @BeforeEach
  public void tearDown() {
    resetDatabase();
  }

  //

  @Test
  public void addHistoryEventLockdown() {
    clock.setFixedTime("2026-04-10T10:00:00Z");
    // Arrange: Expected result.
    SystemHistory expectedHistoryEvent = systemHistoryFactory.genHistoryEvent(null, EnHistoryWho.ADMIN, EnHistoryWhat.LOCKDOWN, "ON");

    // Act: Add system history event to database. Note no user is assigned.
    systemHistoryService.addEvent(null, EnHistoryWho.ADMIN, EnHistoryWhat.LOCKDOWN, "ON");

    // Assert: System history event is in database.
    systemHistoryAssert.assertAll(List.of(expectedHistoryEvent));
  }

  @Test
  public void addHistoryEventForUser() {
    clock.setFixedTime("2026-04-10T10:00:00Z");
    // Arrange: Add user.
    User user = userFactory.genRandUser(EnUserStatus.ACTIVE);
    user = userRepository.save(user);

    // Arrange: Expected result.
    SystemHistory expectedHistoryEvent = systemHistoryFactory.genHistoryEvent(user, EnHistoryWho.ADMIN, EnHistoryWhat.LOCKDOWN, "ON");

    // Act: Add system history event to database assigned to certain user.
    systemHistoryService.addEvent(user.getId(), EnHistoryWho.ADMIN, EnHistoryWhat.LOCKDOWN, "ON");

    // Assert: System history event is in database.
    systemHistoryAssert.assertAll(List.of(expectedHistoryEvent));
  }
}
