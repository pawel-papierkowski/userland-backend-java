package org.portfolio.userland.common.constants;

/**
 * Validation constants.
 */
public class ValidConst {
  /** Regular expression that represents valid email. Example: 'a@b.pl' is valid email. */
  public static final String EMAIL_REGEXPR = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
  /** Regular expression that represents valid or EMPTY email (empty string means 'not provided'). Example: 'a@b.pl' is valid email, '' is valid absence. */
  public static final String EMAIL_OR_EMPTY_REG_EXPR = "^(|[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,})$";

  /** Minimum length of password. */
  public static final int PASS_LEN_MIN = 8;
  /** Maximum length of password. */
  public static final int PASS_LEN_MAX = 100;
  /** Regular expression that represents valid password.
   * Ensures that there is at least one digit, at least one lower-case letter, at least one upper-case letter and
   * at least one special character from list.
   * <p>Example: <code>StrongP@ssw0rd</code> is valid password.</p>
   * Note: size is enforced separately. All <code>@Pattern</code>s with this regexp need to have accompanying
   * <code>@Size</code> annotation. It is necessary due to non-deterministic order of processing annotations on given
   * field. Also, it gives better error message.
   */
  public static final String PASS_REGEXPR = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=.,?!]).*$";

  /** Regular expression that represents valid UUID. Example: 'd9a6075e-85de-4d57-8ba3-3d0d829158fb' is valid UUID. */
  public static final String UUID_REGEXPR = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$";

  /** Minimum length of token. */
  public static final int TOKEN_LEN_MIN = 32;
  /** Maximum length of token. */
  public static final int TOKEN_LEN_MAX = 128;
  /** Regular expression that represents valid token. Example: 'Gl7Y3GK9dqFDEjza3KsOU6k0pM9J4Tiq' is valid token. */
  public static final String TOKEN_REGEXPR = "^[A-Za-z0-9]*$";

  /** Regular expression that represents version number. Example: '0.13.4' or '3.1.0-SNAPSHOT' is valid version. */
  public static final String VERSION_REGEXPR = "^(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)\\.(0|[1-9][0-9]*)(-[a-zA-Z0-9]+)?$";
}
