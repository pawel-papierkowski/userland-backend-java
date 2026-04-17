package org.portfolio.userland.config;

import org.junit.jupiter.api.Test;
import org.portfolio.userland.test.base.BaseIntegrationTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests if I18nConfig works correctly.
 */
public class I18nConfigTest extends BaseIntegrationTest {
  @Autowired
  private MessageSource messageSource;

  @Test
  public void checkMessage() {
    // If "email.test.simple.title" is not known to Spring, it will throw NoSuchMessageException.
    // Language files that contains "email.test.simple.title" are in:
    // for EN: "src/test/resources/i18n/emails/test/test.yaml".
    // for PL: "src/test/resources/i18n/emails/test/test_pl.yaml".
    // there is no file for RU, should fall back to EN.
    String actualTitleEn = messageSource.getMessage("email.test.simple.title", null, Locale.forLanguageTag("en"));
    String actualTitlePl = messageSource.getMessage("email.test.simple.title", null, Locale.forLanguageTag("pl"));
    String actualTitleRu = messageSource.getMessage("email.test.simple.title", null, Locale.forLanguageTag("ru"));
    String expectedTitleEn = "This is title of email";
    String expectedTitlePl = "To jest tytuł emaila";
    String expectedTitleRu = "This is title of email"; // RU is unknown to us, so it should fall back to EN
    assertThat(actualTitleEn).isEqualTo(expectedTitleEn);
    assertThat(actualTitlePl).isEqualTo(expectedTitlePl);
    assertThat(actualTitleRu).isEqualTo(expectedTitleRu);
  }
}
