package org.portfolio.userland.system.history;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.portfolio.userland.system.BaseSystemTest;
import org.portfolio.userland.system.history.entity.EnHistoryWhat;
import org.portfolio.userland.system.history.entity.EnHistoryWho;
import org.portfolio.userland.system.history.entity.SystemHistory;
import org.portfolio.userland.system.history.services.SystemHistoryService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * Tests <code>SystemHistoryService</code>.
 */
public class SystemHistoryServiceTest extends BaseSystemTest {
  @Autowired
  private SystemHistoryService systemHistoryService;

  @AfterEach
  public void tearDown() {
    resetDatabase();
  }

  //

  @Test
  public void addHistoryEvent() {
    clock.setFixedTime("2026-04-10T10:00:00Z");
    // Arrange: Expected result.
    SystemHistory expectedHistoryEvent = systemHistoryFactory.genHistoryEvent(EnHistoryWho.ADMIN, EnHistoryWhat.LOCKDOWN, "ON");

    // Act: Add event.
    systemHistoryService.addEvent(EnHistoryWho.ADMIN, EnHistoryWhat.LOCKDOWN, "ON");

    // Assert: Event is in database.
    systemHistoryAssert.assertAll(List.of(expectedHistoryEvent));
  }
}
