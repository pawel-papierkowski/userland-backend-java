package org.portfolio.userland.features.user.schedulers;

import lombok.RequiredArgsConstructor;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.portfolio.userland.features.user.constants.UserLockConst;
import org.portfolio.userland.features.user.services.standard.UserMaintenanceService;
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
   * Cleans up expired pending users. Removes users with PENDING status that are too old.
   */
  @Scheduled(
      fixedDelayString = "${app.scheduler.user.cleanup.users.pending.delay}",
      initialDelayString = "${app.scheduler.user.cleanup.users.pending.initial}"
  )
  @SchedulerLock(
      name = UserLockConst.CLEAN_PENDING_USERS,
      lockAtLeastFor = "1m", // Prevents extremely fast nodes from running it twice
      lockAtMostFor = "15m"  // Failsafe in case the node dies while holding the lock
  )
  public void cleanPendingUsers() {
    userMaintenanceService.cleanPendingUsers();
  }

  /**
   * Cleans up expired active users. Removes users with ACTIVE status that had last activity too long in past.
   */
  @Scheduled(
      fixedDelayString = "${app.scheduler.user.cleanup.users.active.delay}",
      initialDelayString = "${app.scheduler.user.cleanup.users.active.initial}"
  )
  @SchedulerLock(
      name = UserLockConst.CLEAN_ACTIVE_USERS,
      lockAtLeastFor = "1m", // Prevents extremely fast nodes from running it twice
      lockAtMostFor = "15m"  // Failsafe in case the node dies while holding the lock
  )
  public void cleanActiveUsers() {
    userMaintenanceService.cleanActiveUsers();
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
