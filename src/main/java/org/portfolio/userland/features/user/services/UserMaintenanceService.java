package org.portfolio.userland.features.user.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.portfolio.userland.common.services.clock.ClockService;
import org.portfolio.userland.features.user.repositories.UserJwtRepository;
import org.portfolio.userland.features.user.repositories.UserRepository;
import org.portfolio.userland.features.user.repositories.UserTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StopWatch;

import java.time.LocalDateTime;

/**
 * User maintenance service.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserMaintenanceService {

  private final UserRepository userRepository;
  private final UserTokenRepository userTokenRepository;
  private final UserJwtRepository userJwtRepository;
  private final ClockService clockService;

  /** How long to wait before removal of pending user in hours. */
  @Value("${app.user.pending.removal-delay}")
  private long removalDelay;

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

  @Transactional
  public void cleanExpiredJwts() {
    log.info("Starting scheduled cleanup of expired JWTs...");
    StopWatch stopWatch = new StopWatch("Expired Tokens Cleanup");
    LocalDateTime nowAt = clockService.getNowUTC();

    // Delete tokens that expired naturally.
    stopWatch.start("Delete Expired JWTs");
    int deletedTokens = userJwtRepository.deleteExpiredJwts(nowAt);
    stopWatch.stop();

    log.info("Cleaned up {} expired JWTs. Total time: {} s", deletedTokens, stopWatch.getTotalTimeSeconds());
  }
}
