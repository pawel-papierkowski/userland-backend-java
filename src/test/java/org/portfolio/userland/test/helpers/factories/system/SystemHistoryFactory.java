package org.portfolio.userland.test.helpers.factories.system;

import lombok.RequiredArgsConstructor;
import org.portfolio.userland.features.user.entities.User;
import org.portfolio.userland.system.history.entity.EnHistoryWhat;
import org.portfolio.userland.system.history.entity.EnHistoryWho;
import org.portfolio.userland.system.history.entity.SystemHistory;
import org.portfolio.userland.test.helpers.factories.BaseFactory;
import org.springframework.stereotype.Service;

/**
 * Generates system history event for tests.
 */
@Service
@RequiredArgsConstructor
public class SystemHistoryFactory extends BaseFactory {
  /**
   * Generate system history event.
   * @param who Who did it?
   * @param what What happened?
   * @param value Custom value.
   * @return User history event.
   */
  public SystemHistory genHistoryEvent(User user, EnHistoryWho who, EnHistoryWhat what, String value) {
    SystemHistory systemHistoryEvent = new SystemHistory();
    systemHistoryEvent.setUuid(securityGeneratorService.uuid());
    systemHistoryEvent.setCreatedAt(clockService.getNowUTC());
    systemHistoryEvent.setUser(user);
    systemHistoryEvent.setWho(who);
    systemHistoryEvent.setWhat(what);
    systemHistoryEvent.setValue(value);
    return systemHistoryEvent;
  }
}
