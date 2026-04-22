package org.portfolio.userland.features.config.service;

/**
 * UserLand configuration constants.
 */
public class UlConfigConst {

  // FEATURE: USER

  /** If present and with value 1, no user can log in, unless they have ROLE_ADMIN. */
  public final static String USER_LOCKDOWN = "user.lockdown";
  public final static String USER_LOCKDOWN_DEF = "0"; // Default for above.
}
