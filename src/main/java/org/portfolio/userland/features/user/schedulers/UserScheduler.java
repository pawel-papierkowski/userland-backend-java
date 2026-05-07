package org.portfolio.userland.features.user.schedulers;

import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.portfolio.userland.features.user.constants.UserLockConst;
import org.portfolio.userland.features.user.services.UserMaintenanceService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Scheduler for users. Schedules:
 * <ul>
 *   <li>Cleanup of expired users (PENDING only).</li>
 *   <li>Cleanup of expired tokens.</li>
 *   <li>Cleanup of expired JWT.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class UserScheduler {
  private final UserMaintenanceService userMaintenanceService;

  /**
   * Cleans up expired users. Removes users with PENDING status that are too old.
   */
  @Scheduled(
      fixedDelayString = "${app.scheduler.user.cleanup.users.delay}",
      initialDelayString = "${app.scheduler.user.cleanup.users.initial}"
  )
  @SchedulerLock(
      name = UserLockConst.CLEAN_EXPIRED_USERS,
      lockAtLeastFor = "1m", // Prevents extremely fast nodes from running it twice
      lockAtMostFor = "15m"  // Failsafe in case the node dies while holding the lock
  )
  public void cleanExpiredUsers() {
    userMaintenanceService.cleanExpiredUsers();
  }

  /**
   * Cleans up expired tokens.
   */
  @Scheduled(
      fixedDelayString = "${app.scheduler.user.cleanup.tokens.delay}",
      initialDelayString = "${app.scheduler.user.cleanup.tokens.initial}"
  )
  @SchedulerLock(
      name = UserLockConst.CLEAN_EXPIRED_TOKENS,
      lockAtLeastFor = "1m",
      lockAtMostFor = "15m"
  )
  public void cleanExpiredTokens() {
    userMaintenanceService.cleanExpiredTokens();
  }

  /**
   * Cleans up expired JWTs.
   */
  @Scheduled(
      fixedDelayString = "${app.scheduler.user.cleanup.jwt.delay}",
      initialDelayString = "${app.scheduler.user.cleanup.jwt.initial}"
  )
  @SchedulerLock(
      name = UserLockConst.CLEAN_EXPIRED_JWTS,
      lockAtLeastFor = "1m",
      lockAtMostFor = "15m"
  )
  public void cleanExpiredJwts() {
    userMaintenanceService.cleanExpiredJwts();
  }
}
