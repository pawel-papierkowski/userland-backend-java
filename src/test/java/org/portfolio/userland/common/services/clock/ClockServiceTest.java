package org.portfolio.userland.common.services.clock;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit test for {@link ClockService}.
 */
public class ClockServiceTest {

  // //////////////////////////////////////////////////////////////////////////
  // getInstant

  @Test
  public void getInstantReturnsFixedTime() {
    // Arrange
    Instant fixedInstant = Instant.parse("2026-04-10T10:00:00Z");
    ClockService clockService = new ClockService(Clock.fixed(fixedInstant, ZoneOffset.UTC));

    // Act
    Instant result = clockService.getInstant();

    // Assert
    assertThat(result).isEqualTo(fixedInstant);
  }

  @Test
  public void getInstantReturnsCurrentTime() {
    // Arrange
    ClockService clockService = new ClockService(Clock.systemUTC());

    // Act
    Instant result = clockService.getInstant();
    Instant before = Instant.now();

    // Assert
    assertThat(result).isBetween(before.minusSeconds(1), before.plusSeconds(1));
  }

  // //////////////////////////////////////////////////////////////////////////
  // getNowUTC

  @Test
  public void getNowUTCReturnsFixedTime() {
    // Arrange
    Instant fixedInstant = Instant.parse("2026-04-10T10:00:00Z");
    ClockService clockService = new ClockService(Clock.fixed(fixedInstant, ZoneOffset.UTC));

    // Act
    LocalDateTime result = clockService.getNowUTC();

    // Assert
    assertThat(result).isEqualTo(LocalDateTime.of(2026, 4, 10, 10, 0, 0));
  }

  @Test
  public void getNowUTCReturnsCurrentTime() {
    // Arrange
    ClockService clockService = new ClockService(Clock.systemUTC());

    // Act
    LocalDateTime result = clockService.getNowUTC();
    LocalDateTime before = LocalDateTime.now(ZoneOffset.UTC);

    // Assert
    assertThat(result).isBetween(before.minusSeconds(1), before.plusSeconds(1));
  }

  // //////////////////////////////////////////////////////////////////////////
  // convert(LocalDateTime)

  @Test
  public void convertLocalDateTimeToDate() {
    // Arrange
    ClockService clockService = new ClockService(Clock.systemUTC());
    LocalDateTime dateAt = LocalDateTime.of(2026, 4, 10, 10, 0, 0);

    // Act
    Date result = clockService.convert(dateAt);

    // Assert
    assertThat(result).isEqualTo(Date.from(Instant.parse("2026-04-10T10:00:00Z")));
  }

  @Test
  public void convertLocalDateTimeToEpochMillisZero() {
    // Arrange
    ClockService clockService = new ClockService(Clock.systemUTC());
    LocalDateTime epoch = LocalDateTime.of(1970, 1, 1, 0, 0, 0);

    // Act
    Date result = clockService.convert(epoch);

    // Assert
    assertThat(result.getTime()).isEqualTo(0L);
  }

  // //////////////////////////////////////////////////////////////////////////
  // convert(Long)

  @Test
  public void convertEpochMillisToLocalDateTime() {
    // Arrange
    ClockService clockService = new ClockService(Clock.systemUTC());

    // Act
    LocalDateTime result = clockService.convert(0L);

    // Assert
    assertThat(result).isEqualTo(LocalDateTime.of(1970, 1, 1, 0, 0, 0));
  }

  @Test
  public void convertEpochMillisToLocalDateTimeNonZero() {
    // Arrange
    ClockService clockService = new ClockService(Clock.systemUTC());
    long epochMillis = 1775815200000L; // 2026-04-10T10:00:00Z

    // Act
    LocalDateTime result = clockService.convert(epochMillis);

    // Assert
    assertThat(result).isEqualTo(LocalDateTime.of(2026, 4, 10, 10, 0, 0));
  }
}
