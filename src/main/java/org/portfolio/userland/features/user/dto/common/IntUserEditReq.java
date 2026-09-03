package org.portfolio.userland.features.user.dto.common;

import org.apache.commons.lang3.StringUtils;

/**
 * Interface for all user edit requests. Contains all usable user data, but you can use only some of them.
 * If implementation does not use some field, you need to add method that always return null for that field.
 * Obligatory fields are exception; those must be fully implemented.
 */
public interface IntUserEditReq {
  /** Identifier of user. Obligatory. */
  Long id();
  /** Version for optimistic locking. Obligatory. */
  Long version();

  /** Username. Can be null. */
  String username();
  /** User email. Can be null.  */
  String email();
  /** Locked status. Can be null. */
  Boolean locked();
  /** User language as simple language code. Example: 'pl'. Can be null. */
  String lang();
  /** User profile. Can be null. */
  UserProfileData profile();

  //

  /**
   * Check if at least one field of user data is not empty.
   * @return True if at least one field is not empty, otherwise false.
   */
  default boolean userPresent() {
    if (StringUtils.isNotEmpty(username())) return true;
    if (StringUtils.isNotEmpty(email())) return true;
    if (locked() != null) return true;
    if (StringUtils.isNotEmpty(lang())) return true;
    return false;
  }

  /**
   * Check if at least one field of user profile data is not empty.
   * @return True if at least one field is not empty, otherwise false.
   */
  default boolean userProfilePresent() {
    if (profile() == null) return false;
    return profile().userProfilePresent();
  }
}
