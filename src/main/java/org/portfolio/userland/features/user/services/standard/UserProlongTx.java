package org.portfolio.userland.features.user.services.standard;

import org.portfolio.userland.features.user.entities.EnUserHistoryWhat;
import org.portfolio.userland.features.user.entities.EnUserHistoryWho;
import org.portfolio.userland.features.user.services.BaseUserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Transactional part of user session prolongation. Persists results of successful prolongation: revokes old JWT
 * entries, saves new JWT entry and prolongation history event.
 * <p>It is separated from {@link UserLoginService} so JWT generation can run outside of transaction and not hold
 * a database connection.</p>
 */
@Service
@Transactional
public class UserProlongTx extends BaseUserService {
  /**
   * Revoke all user's JWT entries and save new one with prolongation history event.
   * @param userId Id of user that prolonged session.
   * @param nowAt Current date&time.
   * @param jwtToken Generated JWT token.
   * @param jwtExpire Custom JWT expiration in minutes. Can be null, default expiration will be used.
   */
  public void saveProlong(Long userId, LocalDateTime nowAt, String jwtToken, Long jwtExpire) {
    // Revoke all JWTs related to this user. Note: this is intentional - prolongation consolidates user to single
    // active session, logging out any other devices.
    userJwtRepository.deleteAllByUser(userId);
    // Add new JWT entry in database. This will allow us to effectively revoke tokens later (logout etc).
    addJwtEntry(userRepository.getReferenceById(userId), nowAt, jwtToken, jwtExpire);
    // Add prolong event to user history.
    addHistoryEvent(userId, nowAt, EnUserHistoryWho.USER, EnUserHistoryWhat.PROLONG, "");
  }
}
