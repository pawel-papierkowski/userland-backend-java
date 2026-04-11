package org.portfolio.userland.common.constants;

/**
 * Validation constants.
 */
public class ValidConst {
  /** Regular expression that represents valid email. Example: 'a@b.c' is valid email. */
  public static final String REG_EXPR_EMAIL = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+.[A-Za-z0-9.-]+$";
  /** Regular expression that represents valid UUID. Example: 'd9a6075e-85de-4d57-8ba3-3d0d829158fb' is valid UUID. */
  public static final String REG_EXPR_UUID = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$";
  /** Regular expression that represents valid token. Example: 'Gl7Y3GK9dqFDEjza3KsOU6k0pM9J4Tiq' is valid token. */
  public static final String REG_EXPR_TOKEN = "^[A-Za-z0-9]{32}$";
}
