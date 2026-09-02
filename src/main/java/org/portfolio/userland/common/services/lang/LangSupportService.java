package org.portfolio.userland.common.services.lang;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/**
 * Language service for language-specific rules.
 */
@Service
@RequiredArgsConstructor
public class LangSupportService {
  private final static long MINUTES_IN_HOUR = 60;
  private final LangService langService;

  /**
   * Translate amount of minutes for period. For example, "valid for 5 minutes".
   * Can handle minutes, hours and mixed hours/minutes.
   * @param lang Language code. Example: "pl".
   * @param amount Time amount in minutes.
   * @param onlyMinutes If true, always show time in minutes. If false, can mix hours and minutes.
   * @return Translated amount of time.
   */
  public String tMinutesPeriod(String lang, long amount, boolean onlyMinutes) {
    if (onlyMinutes || amount == 0) return amount + " " + langService.t(lang, resolveMinutesKey(lang, amount));

    // We need to find how many full 60 minutes (hours) are present.
    long hourAmount = amount / MINUTES_IN_HOUR;
    long minuteAmount = amount - hourAmount*MINUTES_IN_HOUR;

    String translationHours = "";
    if (hourAmount != 0) translationHours += hourAmount + " " + langService.t(lang, resolveHoursKey(lang, hourAmount));
    String translationMinutes = "";
    if (minuteAmount != 0) translationMinutes += minuteAmount + " " + langService.t(lang, resolveMinutesKey(lang, minuteAmount));

    if (StringUtils.isNotEmpty(translationHours) && StringUtils.isNotEmpty(translationMinutes)) return translationHours + " " + translationMinutes;
    return translationHours + translationMinutes;
  }

  //

  /**
   * Resolve language key for minute/minutes.
   * @param lang Language code.
   * @param amount Amount.
   * @return Language key.
   */
  private String resolveMinutesKey(String lang, long amount) {
    return resolveKey(lang, "minute", amount);
  }

  /**
   * Resolve language key for hour/hours.
   * @param lang Language code.
   * @param amount Amount.
   * @return Language key.
   */
  private String resolveHoursKey(String lang, long amount) {
    return resolveKey(lang, "hour", amount);
  }

  /**
   * Resolve language key for time unit. Polish has pretty specific rules for end of amount.
   * @param lang Language code.
   * @param unit Time unit.
   * @param amount Amount.
   * @return Language key.
   */
  private String resolveKey(String lang, String unit, long amount) {
    String ending;
    if ("pl".equals(lang)) ending = resolveKeyPl(amount);
    else ending = resolveKeyEn(amount);
    return "units.time."+unit+"."+ending;
  }

  /**
   * Resolve language key for time unit. English is very simple.
   * @param amount Amount.
   * @return Part of language key for ending.
   */
  private String resolveKeyEn(long amount) {
    if (amount == 1 || amount == -1) return "single";
    return "many";
  }

  /**
   * Resolve language key for time unit. Polish has pretty specific rules for ending of amount.
   * @param amount Amount.
   * @return Part of language key for ending.
   */
  private String resolveKeyPl(long amount) {
    // first, we check two last digits of number
    long lastTwoDigits = amount % 100;
    if (lastTwoDigits >= 11 && lastTwoDigits <= 19) return "many";
    if (lastTwoDigits >= -19 && lastTwoDigits <= -11) return "many";

    // now we check last digit of number
    long lastDigit = amount % 10;
    if (lastDigit == 1 || lastDigit == -1) return "single";
    if (lastDigit > 1 && lastDigit < 5) return "few";
    if (lastDigit > -5 && lastDigit < -1) return "few";

    // all other cases
    return "many";
  }
}
