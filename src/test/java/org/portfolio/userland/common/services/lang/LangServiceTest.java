package org.portfolio.userland.common.services.lang;

import org.junit.jupiter.api.Test;
import org.portfolio.userland.test.base.BaseIntegrationTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.NoSuchMessageException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * Tests for {@link LangService}: basic translation, variable substitution, locale fallback, and missing key handling.
 */
public class LangServiceTest extends BaseIntegrationTest {
  @Autowired
  private LangService langService;

  // //////////////////////////////////////////////////////////////////////////
  // Basic translation
  // //////////////////////////////////////////////////////////////////////////

  @Test
  void translate_en() {
    String actual = langService.t("en", "email.test.simple.title");
    assertThat(actual).isEqualTo("This is title of email");
  }

  @Test
  void translate_pl() {
    String actual = langService.t("pl", "email.test.simple.title");
    assertThat(actual).isEqualTo("To jest tytuł emaila");
  }

  @Test
  void unknownLocale_fallsBackToEn() {
    // Russian is not configured, should fall back to English (fallbackToSystemLocale=false in I18nConfig).
    String actual = langService.t("ru", "email.test.simple.title");
    assertThat(actual).isEqualTo("This is title of email");
  }

  // //////////////////////////////////////////////////////////////////////////
  // Translation with arguments
  // //////////////////////////////////////////////////////////////////////////

  @Test
  void translateWithArgs_en() {
    String actual = langService.t("en", "email.test.simple.content", new Object[]{"Alice"});
    assertThat(actual).isEqualTo("This is content of email. Variable: Alice.");
  }

  @Test
  void translateWithArgs_pl() {
    String actual = langService.t("pl", "email.test.simple.content", new Object[]{"Alicja"});
    assertThat(actual).isEqualTo("To jest zawartość emaila. Zmienna: Alicja.");
  }

  @Test
  void translateWithNullArgs() {
    // Null args should be handled the same as no args — placeholder {0} remains unresolved.
    String actual = langService.t("en", "email.test.simple.content", (Object[]) null);
    assertThat(actual).isEqualTo("This is content of email. Variable: {0}.");
  }

  // //////////////////////////////////////////////////////////////////////////
  // Missing key
  // //////////////////////////////////////////////////////////////////////////

  @Test
  void missingKey_throwsException() {
    Throwable thrown = catchThrowable(() -> langService.t("en", "nonexistent.key"));
    assertThat(thrown).isInstanceOf(NoSuchMessageException.class);
  }
}
