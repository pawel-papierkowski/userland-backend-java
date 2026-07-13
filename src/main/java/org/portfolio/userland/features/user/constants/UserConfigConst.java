package org.portfolio.userland.features.user.constants;

/**
 * User configuration constants.
 */
public class UserConfigConst {
  /** Custom expiration of JWT in minutes. */
  public final static String JWT_EXPIRE = "jwt.expire";

  /** If present and 1, this user will never be removed in portfolio mode. */
  public final static String PORTFOLIO_NODELETE = "portfolio.noDelete";
}
