package org.portfolio.userland.system.config.service;

/**
 * UserLand configuration constants.
 */
public class ConfigConst {
  public final static String TRUE = "1";
  public final static String FALSE = "0";

  // FEATURE: USER

  /** If present and with value 1, no user can log in, unless they have ROLE_ADMIN. */
  public final static String USER_LOCKDOWN = "user.lockdown";
  public final static String USER_LOCKDOWN_DEF = "0"; // Default for above.
}
