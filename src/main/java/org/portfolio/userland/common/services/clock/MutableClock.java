package org.portfolio.userland.common.services.clock;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

/**
 * Clock that can be set to any time. Useful in tests.
 */
public class MutableClock extends Clock {
  /** Actual clock under the wraps. */
  private Clock delegate = Clock.systemUTC();

  /**
   * Set date and time to any valid value.
   * @param instantStr Date&time in ISO format. Example: '2007-12-03T10:15:30.00Z'
   */
  public void setFixedTime(String instantStr) {
    this.delegate = Clock.fixed(Instant.parse(instantStr), ZoneId.of("UTC"));
  }

  /**
   * Move time by given duration.
   * @param duration Duration.
   */
  public void advanceTime(Duration duration) {
    this.delegate = Clock.offset(this.delegate, duration);
  }

  /**
   * Reset time to current date&time.
   */
  public void reset() {
    this.delegate = Clock.systemUTC();
  }

  @Override
  public ZoneId getZone() { return delegate.getZone(); }

  @Override
  public Clock withZone(ZoneId zone) { return delegate.withZone(zone); }

  @Override
  public Instant instant() { return delegate.instant(); }
}
