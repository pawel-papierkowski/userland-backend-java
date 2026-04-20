package org.portfolio.userland.test.helpers.factories;

import lombok.RequiredArgsConstructor;
import org.portfolio.userland.common.services.clock.ClockService;
import org.portfolio.userland.common.services.security.SecurityGeneratorService;
import org.portfolio.userland.features.user.entity.EnHistoryWhat;
import org.portfolio.userland.features.user.entity.EnHistoryWho;
import org.portfolio.userland.features.user.entity.User;
import org.portfolio.userland.features.user.entity.UserHistory;
import org.springframework.stereotype.Service;

/**
 * Generates user history event for tests.
 */
@Service
@RequiredArgsConstructor
public class UserHistoryFactory {
  private final SecurityGeneratorService securityGeneratorService;
  private final ClockService clockService;

  /**
   * Generate user history event and assign it to user.
   * @param user User.
   * @param what What happened?
   * @return User history event.
   */
  public UserHistory genHistoryEvent(User user, EnHistoryWhat what) {
    UserHistory userHistory = new UserHistory();
    userHistory.setUuid(securityGeneratorService.uuid());
    userHistory.setCreatedAt(clockService.getNowUTC());
    userHistory.setWho(EnHistoryWho.USER);
    userHistory.setWhat(what);
    user.addHistory(userHistory);
    return userHistory;
  }
}
