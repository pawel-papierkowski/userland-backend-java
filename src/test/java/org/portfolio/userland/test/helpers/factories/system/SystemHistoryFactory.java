package org.portfolio.userland.test.helpers.factories.system;

import lombok.RequiredArgsConstructor;
import org.portfolio.userland.features.user.entities.User;
import org.portfolio.userland.system.history.entities.EnHistoryWhat;
import org.portfolio.userland.system.history.entities.EnHistoryWho;
import org.portfolio.userland.system.history.entities.SystemHistory;
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
   * @param params Event parameters.
   * @return System history event.
   */
  public SystemHistory genHistoryEvent(User user, EnHistoryWho who, EnHistoryWhat what, String params) {
    SystemHistory systemHistoryEvent = new SystemHistory();
    systemHistoryEvent.setUuid(securityGeneratorService.uuid());
    systemHistoryEvent.setCreatedAt(clockService.getNowUTC());
    systemHistoryEvent.setUser(user);
    systemHistoryEvent.setWho(who);
    systemHistoryEvent.setWhat(what);
    systemHistoryEvent.setParams(params);
    return systemHistoryEvent;
  }
}
