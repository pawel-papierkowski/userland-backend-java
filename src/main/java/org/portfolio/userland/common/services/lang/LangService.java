package org.portfolio.userland.common.services.lang;

import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;

import java.util.Locale;

/**
 * Language service for manual translations.
 */
@Service
@RequiredArgsConstructor
public class LangService {
  private final MessageSource messageSource;

  /**
   * Translate given key using given language.
   * @param lang Language code. Example: "pl".
   * @param key Translation key. Example: "user.email.registration.greeting".
   * @return Translated text.
   */
  public String t(String lang, String key) {
    return t(lang, key, null);
  }

  /**
   * Translate given key using given language.
   * You can use variables in translation. Example: "Hello, {0}!".
   * @param lang Language code. Example: "pl".
   * @param key Translation key. Example: "user.email.registration.greeting".
   * @param args Variables used in key. Can be null if no variables used in key.
   * @return Translated text.
   */
  public String t(String lang, String key, Object[] args) {
    Locale locale = Locale.forLanguageTag(lang);
    return messageSource.getMessage(key, args, locale);
  }
}
