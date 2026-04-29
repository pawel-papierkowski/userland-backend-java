package org.portfolio.userland.test.helpers.factories.user;

import lombok.RequiredArgsConstructor;
import org.portfolio.userland.features.user.entities.EnUserHistoryWhat;
import org.portfolio.userland.features.user.entities.EnUserHistoryWho;
import org.portfolio.userland.features.user.entities.User;
import org.portfolio.userland.features.user.entities.UserHistory;
import org.portfolio.userland.test.helpers.factories.BaseFactory;
import org.springframework.stereotype.Service;

/**
 * Generates user history event for tests.
 */
@Service
@RequiredArgsConstructor
public class UserHistoryFactory extends BaseFactory {
  /**
   * Generate user history event and assign it to user.
   *
   * @param user User.
   * @param what What happened?
   * @param params History event parameters.
   * @return User history event.
   */
  public UserHistory genHistoryEvent(User user, EnUserHistoryWhat what, String params) {
    UserHistory userHistory = new UserHistory();
    userHistory.setUuid(securityGeneratorService.uuid());
    userHistory.setCreatedAt(clockService.getNowUTC());
    userHistory.setWho(EnUserHistoryWho.USER);
    userHistory.setWhat(what);
    userHistory.setParams(params);
    user.addHistory(userHistory);
    return userHistory;
  }
}
