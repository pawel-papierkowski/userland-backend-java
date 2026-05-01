package org.portfolio.userland.features.user.mappers;

import org.mapstruct.Mapper;
import org.portfolio.userland.features.user.dto.common.UserProfileDataResp;
import org.portfolio.userland.features.user.entities.UserProfile;

/**
 * Maps requests to <code>UserProfile</code> and <code>UserProfile</code> to responses.
 */
@Mapper(componentModel = "spring")
public abstract class UserProfileMapper {
  /**
   * Maps <code>UserProfile</code> entity to user profile data response.
   * @param userProfile <code>UserProfile</code> entity.
   * @return User profile data response.
   */
  public abstract UserProfileDataResp profileToDataResp(UserProfile userProfile);
}
