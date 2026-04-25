package org.portfolio.userland.test.helpers.asserts;

import lombok.RequiredArgsConstructor;
import org.portfolio.userland.common.constants.ValidConst;
import org.portfolio.userland.system.history.entity.SystemHistory;
import org.portfolio.userland.system.history.repositories.SystemHistoryRepository;
import org.springframework.stereotype.Service;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Asserts system history event.
 */
@Service
@RequiredArgsConstructor
public class SystemHistoryAssert {
  private static final String[] SYSTEM_HISTORY_FIELDS_IGNORE = { "id", "uuid" };

  private final SystemHistoryRepository systemHistoryRepository;

  /**
   * Assert that all system history events currently in database match provided expected list of system history events.
   * @param expectedHistoryEvents Expected system history events.
   */
  public void assertAll(List<SystemHistory> expectedHistoryEvents) {
    List<SystemHistory> actualHistoryEvents = systemHistoryRepository.findAll();
    assertThat(actualHistoryEvents.size()).as("System history events must have valid count").isEqualTo(expectedHistoryEvents.size());

    for (int i=0; i<actualHistoryEvents.size(); i++) {
      SystemHistory actualHistoryEvent = actualHistoryEvents.get(i);
      SystemHistory expectedHistoryEvent = expectedHistoryEvents.get(i);
      assertIt("SystemHistory["+i+"]", actualHistoryEvent, expectedHistoryEvent);
    }
  }

  /**
   * Assert that two system history events are same.
   * @param actualHistoryEvent Actual system history event.
   * @param expectedHistoryEvent Expected system history event.
   */
  public void assertIt(SystemHistory actualHistoryEvent, SystemHistory expectedHistoryEvent) {
    assertIt("SystemHistory", actualHistoryEvent, expectedHistoryEvent);
  }

  /**
   * Assert that two system history events are same.
   * @param comment Comment.
   * @param actualHistoryEvent Actual system history event.
   * @param expectedHistoryEvent Expected system history event.
   */
  public void assertIt(String comment, SystemHistory actualHistoryEvent, SystemHistory expectedHistoryEvent) {
    assertThat(actualHistoryEvent)
        .as(comment + ": Is different")
        .usingRecursiveComparison()
        .ignoringFields(SYSTEM_HISTORY_FIELDS_IGNORE)
        .isEqualTo(expectedHistoryEvent);

    // Assert fields that need to be asserted separately for various reasons
    assertThat(actualHistoryEvent.getId()).as(comment + ": Id is wrong").isGreaterThan(0L);
    assertThat(actualHistoryEvent.getUuid()).as(comment + ": Event UUID is invalid").matches(ValidConst.REG_EXPR_UUID);
  }
}
