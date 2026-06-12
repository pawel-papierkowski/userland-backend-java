package org.portfolio.userland.features.user.constants;

/**
 * Constants for users: error codes.
 */
public class UserErrCode {
  /** User not found. */
  public final static String NOT_FOUND = "user_0001";

  /** User token is missing. */
  public final static String TOKEN_MISSING = "user_0011";
  /** User token is expired. */
  public final static String TOKEN_EXPIRED = "user_0012";
  /** User token already exists. */
  public final static String TOKEN_ALREADY = "user_0013";

  /** User config entry is missing. */
  public final static String CONFIG_MISSING = "user_0021";
  /** User config entry is redundant. */
  public final static String CONFIG_REDUNDANT = "user_0022";

  /** User permission entry is missing. */
  public final static String PERMISSION_USER_MISSING = "user_0031";
  /** User permission entry is redundant. */
  public final static String PERMISSION_USER_REDUNDANT = "user_0032";

  /** Permission entry is missing. */
  public final static String PERMISSION_MISSING = "user_0041";

  /** Email already exists. */
  public final static String EMAIL_IN_USE = "user_0111";
  /** Wrong password or account. */
  public final static String WRONG_PASSWORD = "user_0112";

  /** User has invalid status. */
  public final static String INVALID_STATUS = "user_0121";
  /** User is locked. */
  public final static String LOCKED = "user_0122";

  // ADMIN PANEL SPECIFIC

  /** User cannot be edited. */
  public final static String CANNOT_EDIT = "user_0201";
}
