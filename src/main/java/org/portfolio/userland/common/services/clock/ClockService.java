package org.portfolio.userland.common.services.clock;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.Date;

/**
 * <p>Wraps <code>Clock</code> and provides various useful utility functions. Application should use this service
 * instead of using standard clock or other date/time retrieval methods.</p>
 * <p>Note: system operates on UTC time. Frontend should do appropriate conversion to local date&time as needed.</p>
 */
@Service
@RequiredArgsConstructor
public class ClockService {
  private final static ZoneId utcZone = ZoneOffset.UTC;

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
   * @return Date.
   */
  public Date convert(LocalDateTime dateAt) {
    return Date.from(dateAt.atZone(utcZone).toInstant());
  }

  /**
   * Convert epoch to LocalDateTime.
   * @param epochMilli The number of milliseconds from 1970-01-01T00:00:00Z.
   * @return Local date and time.
   */
  public LocalDateTime convert(Long epochMilli) {
    return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMilli), utcZone);
  }
}
