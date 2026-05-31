package org.portfolio.userland.test.helpers.factories.user.admin;

import lombok.RequiredArgsConstructor;
import org.portfolio.userland.features.user.dto.admin.config.UserConfigTableEntry;
import org.portfolio.userland.features.user.dto.admin.user.UserFullDataResp;
import org.portfolio.userland.features.user.dto.admin.user.UserTableEntry;
import org.portfolio.userland.features.user.dto.common.UserProfileDataResp;
import org.portfolio.userland.features.user.entities.User;
import org.portfolio.userland.features.user.entities.UserConfig;
import org.portfolio.userland.features.user.entities.UserProfile;
import org.portfolio.userland.test.helpers.factories.BaseFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Generates user data from admin endpoints based on user entity.
 */
@Service
@RequiredArgsConstructor
public class UserAdminFactory extends BaseFactory {
  /**
   * Create user table entries based on user entities.
   * @param entities List of user entities.
   * @return List ot user table entries.
   */
  public List<UserTableEntry> genUserTableEntries(List<User> entities) {
    List<UserTableEntry> entries = new ArrayList<>();
    for (User entity : entities) {
      UserTableEntry entry = genUserTableEntries(entity);
      entries.add(entry);
    }
    return entries;
  }

  /**
   * Create user table entry based on user entity.
   * @param entity User entity.
   * @return User table entry.
   */
  private UserTableEntry genUserTableEntries(User entity) {
    // Build manually. Actual code uses mapper.
    return UserTableEntry.builder()
        .id(entity.getId())
        .createdAt(entity.getCreatedAt())
        .username(entity.getUsername())
        .email(entity.getEmail())
        .build();
  }

  //

  /**
   * Generate full data response based on user and user profile.
   * @param user User entity.
   * @param userProfile User profile entity.
   * @return Full user data response.
   */
  public UserFullDataResp genFullData(User user, UserProfile userProfile) {
    // Build manually. Actual code uses mapper.
    UserProfileDataResp profile = UserProfileDataResp.builder()
        .name(userProfile.getName())
        .surname(userProfile.getSurname())
        .build();
    return UserFullDataResp.builder()
        .id(user.getId())
        .createdAt(user.getCreatedAt())
        .modifiedAt(user.getModifiedAt())
        .username(user.getUsername())
        .email(user.getEmail())
        .status(user.getStatus())
        .locked(user.getLocked())
        .lang(user.getLang())
        .profile(profile)
        .build();
  }

  //
  /**
   * Create user config table entries based on user config entities.
   * @param entities List of user config entities.
   * @return List ot user config table entries.
   */
  public List<UserConfigTableEntry> genUserConfigTableEntries(List<UserConfig> entities) {
    List<UserConfigTableEntry> entries = new ArrayList<>();
    for (UserConfig entity : entities) {
      UserConfigTableEntry entry = genUserConfigTableEntries(entity);
      entries.add(entry);
    }
    return entries;
  }

  /**
   * Create user config table entry based on user config entity.
   * @param entity User config entity.
   * @return User config table entry.
   */
  private UserConfigTableEntry genUserConfigTableEntries(UserConfig entity) {
    // Build manually. Actual code uses mapper.
    return UserConfigTableEntry.builder()
        .id(entity.getId())
        .createdAt(entity.getCreatedAt())
        .name(entity.getName())
        .value(entity.getValue())
        .build();
  }
}
