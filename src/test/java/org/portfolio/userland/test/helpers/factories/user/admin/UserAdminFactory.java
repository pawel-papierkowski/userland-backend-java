package org.portfolio.userland.test.helpers.factories.user.admin;

import lombok.RequiredArgsConstructor;
import org.portfolio.userland.features.user.dto.admin.user.UserFullDataResp;
import org.portfolio.userland.features.user.dto.admin.user.UserTableEntry;
import org.portfolio.userland.features.user.dto.common.UserProfileDataResp;
import org.portfolio.userland.features.user.entities.User;
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
   * Create user table entries based on users.
   * @param users List of users.
   * @return List ot user table entries.
   */
  public List<UserTableEntry> genUserTableEntries(List<User> users) {
    List<UserTableEntry> resultList = new ArrayList<>();
    for (User user : users) {
      UserTableEntry entry = genUserTableEntries(user);
      resultList.add(entry);
    }
    return resultList;
  }

  /**
   * Create user table entry based on user entity.
   * @param user User.
   * @return User table entry.
   */
  private UserTableEntry genUserTableEntries(User user) {
    // Build manually. Actual code uses mapper.
    return UserTableEntry.builder()
        .id(user.getId())
        .createdAt(user.getCreatedAt())
        .username(user.getUsername())
        .email(user.getEmail())
        .build();
  }

  //

  /**
   * Generate full data response based on user and user profile.
   * @param user User.
   * @param userProfile User profile.
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
}
