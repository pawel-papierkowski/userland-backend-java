package org.portfolio.userland.common.services.clock;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;

/**
 * Wraps Clock and provides various useful utility functions.
 */
@Service
@RequiredArgsConstructor
public class ClockService {
  /** Clock bean configured in ClockConfig. */
  private final Clock clock;

  /**
   * Get current instant.
   * @return Instant representing 'now'.
   */
  public Instant getInstant() {
    return Instant.now(clock);
  }

  /**
   * Get current date and time as UTC.
   * @return Local date&time representing 'now'.
   */
  public LocalDateTime getNowUTC() {
    return LocalDateTime.now(clock);
  }
}
