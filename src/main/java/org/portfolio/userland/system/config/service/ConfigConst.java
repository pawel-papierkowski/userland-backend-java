package org.portfolio.userland.system.config.service;

import com.google.common.collect.Maps;

import java.util.Map;

/**
 * System configuration constants.
 * <p>Note: all <code>configService.get()</code> must use these constants. All config names are paired with default
 * values. It is forbidden to have different defaults for same config name.</p>
 */
public class ConfigConst {
  /** Maps names to default values. Also, useful for enforcing known config names. */
  public final static Map<String, String> DEFAULTS = genDefaults();

  /**
   * Generate defaults for config names.
   * @return Defaults.
   */
  private static Map<String, String> genDefaults() {
    Map<String, String> defaultsMap = Maps.newHashMap();
    // general
    defaultsMap.put(GENERAL_PORTFOLIO, GENERAL_PORTFOLIO_DEF);

    // feature: user
    defaultsMap.put(USER_LOCKDOWN, USER_LOCKDOWN_DEF);

    // tests
    defaultsMap.put(TEST_VAR, TEST_VAR_DEF);
    defaultsMap.put(TEST_CACHE, TEST_CACHE_DEF);
    return defaultsMap;
  }

  //

  public final static String TRUE = "1";
  public final static String FALSE = "0";

  // GENERAL CONFIG

  /** If present and with value 1, system is in portfolio mode. */
  public final static String GENERAL_PORTFOLIO = "general.portfolio";
  public final static String GENERAL_PORTFOLIO_DEF = FALSE; // Default for above.

  // FEATURE: USER

  /** If present and with value 1, no user can log in, unless they have ROLE_ADMIN or ROLE_OPERATOR. */
  public final static String USER_LOCKDOWN = "user.lockdown";
  public final static String USER_LOCKDOWN_DEF = FALSE; // Default for above.

  // TESTS

  public final static String TEST_VAR = "test.var";
  public final static String TEST_VAR_DEF = "test.val"; // Default for above.
  public final static String TEST_CACHE = "test.cache";
  public final static String TEST_CACHE_DEF = "original"; // Default for above.
}
