package org.portfolio.userland.features.user.services.standard;

import org.portfolio.userland.features.user.entities.EnUserHistoryWhat;
import org.portfolio.userland.features.user.entities.EnUserHistoryWho;
import org.portfolio.userland.features.user.services.BaseUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Transactional part of user login. Persists results of successful login: JWT entry and login history event.
 * <p>It is separated from {@link UserLoginService} so expensive CPU operations (BCrypt password verification, JWT
 * generation) can run outside of transaction and not hold a database connection.</p>
 */
@Service
@Transactional
public class UserLoginTx extends BaseUserService {
  /**
   * Save JWT entry and login history event.
   * @param userId Id of user that logged in.
   * @param nowAt Current date&time.
   * @param jwtToken Generated JWT token.
   * @param jwtExpire Custom JWT expiration in minutes. Can be null, default expiration was used.
   * @param httpParams Resolved HTTP parameters for history event.
   */
  public void saveLogin(Long userId, LocalDateTime nowAt, String jwtToken, Long jwtExpire, String httpParams) {
    // Add JWT in database. This will allow us to effectively revoke tokens later (logout etc).
    addJwtEntry(userRepository.getReferenceById(userId), nowAt, jwtToken, jwtExpire);
    // Add login event to user history.
    addHistoryEvent(userId, nowAt, EnUserHistoryWho.USER, EnUserHistoryWhat.LOGIN, httpParams);
  }
}
