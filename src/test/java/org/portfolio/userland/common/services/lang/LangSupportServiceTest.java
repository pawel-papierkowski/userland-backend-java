package org.portfolio.userland.common.services.lang;

import org.junit.jupiter.api.Test;
import org.portfolio.userland.test.base.BaseIntegrationTest;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

public class LangSupportServiceTest extends BaseIntegrationTest {
  @Autowired
  private LangSupportService langSupportService;

  private void verify(long amount, boolean onlyMinutes, String expectedTextEn, String expectedTextPl) {
    // Act: Get translation.
    String actualTextEn = langSupportService.tMinutesPeriod("en", amount, onlyMinutes);
    String actualTextPl = langSupportService.tMinutesPeriod("pl", amount, onlyMinutes);

    // Assert: Ensure expected translation.
    assertThat(actualTextEn).isEqualTo(expectedTextEn);
    assertThat(actualTextPl).isEqualTo(expectedTextPl);
  }

  //

  @Test
  public void minutesOnly() {
    // Check translation for minutes only.
    verify(1, true, "1 minute", "1 minutę");
    verify(3, true, "3 minutes", "3 minuty");
    verify(7, true, "7 minutes", "7 minut");
    verify(10, true, "10 minutes", "10 minut");
    verify(15, true, "15 minutes", "15 minut");
    verify(23, true, "23 minutes", "23 minuty");
    verify(25, true, "25 minutes", "25 minut");
    verify(60, true, "60 minutes", "60 minut");
    verify(90, true, "90 minutes", "90 minut");
    verify(111, true, "111 minutes", "111 minut");
    verify(121, true, "121 minutes", "121 minutę");
    verify(303, true, "303 minutes", "303 minuty");
    verify(1440, true, "1440 minutes", "1440 minut");

    verify(0, true, "0 minutes", "0 minut");
    verify(-1, true, "-1 minute", "-1 minutę");
    verify(-2, true, "-2 minutes", "-2 minuty");
    verify(-5, true, "-5 minutes", "-5 minut");
    verify(-60, true, "-60 minutes", "-60 minut");
  }

  @Test
  public void minutesAndHours() {
    // Check translation for mixed minutes and hours.
    verify(1, false, "1 minute", "1 minutę");
    verify(4, false, "4 minutes", "4 minuty");
    verify(12, false, "12 minutes", "12 minut");
    verify(59, false, "59 minutes", "59 minut");
    verify(60, false, "1 hour", "1 godzinę");
    verify(90, false, "1 hour 30 minutes", "1 godzinę 30 minut");
    verify(121, false, "2 hours 1 minute", "2 godziny 1 minutę");
    verify(303, false, "5 hours 3 minutes", "5 godzin 3 minuty");
    verify(1440, false, "24 hours", "24 godziny");

    verify(0, false, "0 minutes", "0 minut");
    verify(-1, false, "-1 minute", "-1 minutę");
    verify(-2, false, "-2 minutes", "-2 minuty");
    verify(-5, false, "-5 minutes", "-5 minut");
    verify(-60, false, "-1 hour", "-1 godzinę");
  }
}
