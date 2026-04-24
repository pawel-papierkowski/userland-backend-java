package org.portfolio.userland.common.services.clock;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

/**
 * Wraps <code>Clock</code> and provides various useful utility functions. Application should use this service instead
 * of using standard clock or other date/time retrieval methods.
 */
@Service
@RequiredArgsConstructor
public class ClockService {
  /** <code>Clock</code> bean is configured in <code>ClockConfig</code>. */
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

  //

  /**
   * Convert LocalDateTime to Date.
   * @param dateAt LocalDateTime.
   * @return Date
   */
  public Date convert(LocalDateTime dateAt) {
    return Date.from(dateAt.atZone(ZoneId.systemDefault()).toInstant());
  }
}
