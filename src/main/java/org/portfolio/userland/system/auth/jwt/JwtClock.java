package org.portfolio.userland.system.auth.jwt;

import io.jsonwebtoken.Clock;
import lombok.RequiredArgsConstructor;
import org.portfolio.userland.common.services.clock.ClockService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Date;

/**
 * Clock implementation for JWT that allows arbitrary time setting in tests.
 */
@Service
@RequiredArgsConstructor
public class JwtClock implements Clock {
  private final ClockService clockService;

  @Override
  public Date now() {
    // Convert LocalDateTime from clockService to Date.
    LocalDateTime nowAt = clockService.getNowUTC();
    return clockService.convert(nowAt);
  }
}
