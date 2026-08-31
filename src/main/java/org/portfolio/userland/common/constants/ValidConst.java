package org.portfolio.userland.common.constants;

/**
 * Validation constants.
 */
public class ValidConst {
  /** Regular expression that represents valid email. Example: 'a@b.pl' is valid email. */
  public static final String REG_EXPR_EMAIL = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
  /** Regular expression that represents valid or EMPTY email (empty string means 'not provided'). Example: 'a@b.pl' is valid email, '' is valid absence. */
  public static final String REG_EXPR_EMAIL_OR_EMPTY = "^(|[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,})$";

  /** Minimum length of password. */
  public static final int PASS_LEN_MIN = 8;
  /** Maximum length of password. */
  public static final int PASS_LEN_MAX = 100;

  /** Regular expression that represents valid password.
   * Ensure that there is at least one digit, at least one lower-case letter, at least one upper-case letter and
   * at least one special character from list. Also, there must be at least 8 characters.
   * Example: 'StrongP@ssw0rd' is valid password.
   */
  public static final String REG_EXPR_PASSWORD = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=.,?!]).{"+PASS_LEN_MIN+","+PASS_LEN_MAX+"}$";

  /** Regular expression that represents valid UUID. Example: 'd9a6075e-85de-4d57-8ba3-3d0d829158fb' is valid UUID. */
  public static final String REG_EXPR_UUID = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$";
  /** Regular expression that represents valid token. Example: 'Gl7Y3GK9dqFDEjza3KsOU6k0pM9J4Tiq' is valid token. */
  public static final String REG_EXPR_TOKEN = "^[A-Za-z0-9]{32}$";
  /** Regular expression that represents version number. Example: '0.13.4' or '3.1.0-SNAPSHOT' is valid version. */
  public static final String REG_EXPR_VERSION = "^(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)(-[a-zA-Z0-9]+)?$";
}
