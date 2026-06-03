package org.portfolio.userland.features.user.constants;

/**
 * Constants for users: error codes.
 */
public class UserErrCode {
  /** User not found. */
  public final static String NOT_FOUND = "user_0001";

  /** Token is expired. */
  public final static String TOKEN_EXPIRED = "user_0011";
  /** Token is missing. */
  public final static String TOKEN_MISSING = "user_0012";
  /** Token already exists. */
  public final static String TOKEN_ALREADY = "user_0013";

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
