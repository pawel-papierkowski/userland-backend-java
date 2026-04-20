package org.portfolio.userland.features.user.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.portfolio.userland.common.services.clock.ClockService;
import org.portfolio.userland.features.user.repositories.UserRepository;
import org.portfolio.userland.features.user.repositories.UserTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StopWatch;

import java.time.LocalDateTime;

/**
 * Scheduler for users.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserScheduler {
  private final UserRepository userRepository;
  private final UserTokenRepository userTokenRepository;
  private final ClockService clockService;

  /** How long to wait before removal of pending user in hours. */
  @Value("${app.user.pending.removal-delay}")
  private long removalDelay;

  /**
   * Cleans up expired users. Removes users with PENDING status that are too old.
   */
  @Scheduled(
      fixedDelayString = "${app.scheduler.user.cleanup.users.delay}",
      initialDelayString = "${app.scheduler.user.cleanup.users.initial}"
  )
  @SchedulerLock(
      name = "CleanupScheduler_cleanExpiredUsers",
      lockAtLeastFor = "1m", // Prevents extremely fast nodes from running it twice
      lockAtMostFor = "15m"  // Failsafe in case the node dies while holding the lock
  )
  @Transactional
  public void cleanExpiredUsers() {
    log.info("Starting scheduled cleanup of expired users...");
    StopWatch stopWatch = new StopWatch("Expired Users Cleanup");
    LocalDateTime nowAt = clockService.getNowUTC();

    // Delete pending users who never activated.
    stopWatch.start("Delete Expired Users");
    LocalDateTime userCutoffAt = nowAt.minusHours(removalDelay);
    int deletedUsers = userRepository.deletePendingUsersOlderThan(userCutoffAt);
    stopWatch.stop();

    log.info("Cleaned up {} expired pending users. Total time: {} s", deletedUsers, stopWatch.getTotalTimeSeconds());
  }

  /**
   * Cleans up expired tokens.
   */
  @Scheduled(
      fixedDelayString = "${app.scheduler.user.cleanup.tokens.delay}",
      initialDelayString = "${app.scheduler.user.cleanup.tokens.initial}"
  )
  @SchedulerLock(
      name = "CleanupScheduler_cleanExpiredTokens",
      lockAtLeastFor = "1m",
      lockAtMostFor = "15m"
  )
  @Transactional
  public void cleanExpiredTokens() {
    log.info("Starting scheduled cleanup of expired tokens...");
    StopWatch stopWatch = new StopWatch("Expired Tokens Cleanup");
    LocalDateTime nowAt = clockService.getNowUTC();

    // Delete tokens that expired naturally.
    stopWatch.start("Delete Expired Tokens");
    int deletedTokens = userTokenRepository.deleteExpiredTokens(nowAt);
    stopWatch.stop();

    log.info("Cleaned up {} expired tokens. Total time: {} s", deletedTokens, stopWatch.getTotalTimeSeconds());
  }
}
