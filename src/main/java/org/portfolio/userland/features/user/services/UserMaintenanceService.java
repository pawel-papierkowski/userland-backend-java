package org.portfolio.userland.features.user.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.portfolio.userland.common.services.clock.ClockService;
import org.portfolio.userland.features.user.repositories.jwt.UserJwtRepository;
import org.portfolio.userland.features.user.repositories.token.UserTokenRepository;
import org.portfolio.userland.features.user.repositories.user.UserRepository;
import org.portfolio.userland.system.config.service.ConfigConst;
import org.portfolio.userland.system.config.service.ConfigService;
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

  private final ConfigService configService;
  private final ClockService clockService;

  /** How long to wait before removal of pending user in hours. */
  @Value("${app.user.cleanup.pending.removal-delay}")
  private long pendingRemovalDelay;
  /** How long to wait before removal of active user in hours. */
  @Value("${app.user.cleanup.active.removal-delay}")
  private long activeRemovalDelay;

  @Transactional
  public void cleanPendingUsers() {
    log.info("Starting scheduled cleanup of expired pending users...");
    StopWatch stopWatch = new StopWatch("Expired Pending Users Cleanup");
    LocalDateTime nowAt = clockService.getNowUTC();

    // Delete pending users who never activated.
    stopWatch.start("Delete Expired Pending Users");
    LocalDateTime userCutoffAt = nowAt.minusHours(pendingRemovalDelay);
    int deletedUsers = userRepository.deletePendingUsersOlderThan(userCutoffAt);
    stopWatch.stop();

    log.info("Cleaned up {} expired pending users. Total time: {} s", deletedUsers, stopWatch.getTotalTimeSeconds());
  }

  @Transactional
  public void cleanActiveUsers() {
    // Only in portfolio mode.
    String generalPortfolio = configService.get(ConfigConst.GENERAL_PORTFOLIO, "0");
    if (!ConfigConst.TRUE.equals(generalPortfolio)) return;

    log.info("Starting scheduled cleanup of expired active users...");
    StopWatch stopWatch = new StopWatch("Expired Active Users Cleanup");
    LocalDateTime nowAt = clockService.getNowUTC();

    // Delete active users who were idle too long.
    stopWatch.start("Delete Expired Active Users");
    LocalDateTime userCutoffAt = nowAt.minusHours(activeRemovalDelay);
    int deletedUsers = userRepository.deleteActiveUsers(userCutoffAt);
    stopWatch.stop();

    log.info("Cleaned up {} expired active users. Total time: {} s", deletedUsers, stopWatch.getTotalTimeSeconds());
  }

  //

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
