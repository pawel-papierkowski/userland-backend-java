package org.portfolio.userland.test.helpers.factories.user.admin;

import lombok.RequiredArgsConstructor;
import org.portfolio.userland.features.user.dto.admin.config.UserConfigTableEntry;
import org.portfolio.userland.features.user.dto.admin.history.UserHistoryTableEntry;
import org.portfolio.userland.features.user.dto.admin.jwt.UserJwtTableEntry;
import org.portfolio.userland.features.user.dto.admin.permission.UserPermissionTableEntry;
import org.portfolio.userland.features.user.dto.admin.token.UserTokenTableEntry;
import org.portfolio.userland.features.user.dto.admin.user.UserFullDataResp;
import org.portfolio.userland.features.user.dto.admin.user.UserTableEntry;
import org.portfolio.userland.features.user.dto.common.UserProfileDataResp;
import org.portfolio.userland.features.user.entities.*;
import org.portfolio.userland.test.helpers.factories.BaseFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

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
      UserConfigTableEntry entry = genUserConfigTableEntry(entity);
      entries.add(entry);
    }
    return entries;
  }

  /**
   * Create user config table entry based on user config entity.
   * @param entity User config entity.
   * @return User config table entry.
   */
  private UserConfigTableEntry genUserConfigTableEntry(UserConfig entity) {
    // Build manually. Actual code uses mapper.
    return UserConfigTableEntry.builder()
        .id(entity.getId())
        .createdAt(entity.getCreatedAt())
        .name(entity.getName())
        .value(entity.getValue())
        .build();
  }

  //

  /**
   * Create user history table entries based on user history events.
   * @param entities List of user history events.
   * @return List ot user history table entries.
   */
  public List<UserHistoryTableEntry> genUserHistoryTableEntries(List<UserHistory> entities) {
    List<UserHistoryTableEntry> entries = new ArrayList<>();
    for (UserHistory entity : entities) {
      UserHistoryTableEntry entry = genUserHistoryTableEntry(entity);
      entries.add(entry);
    }
    return entries;
  }

  /**
   * Create user history table entry based on user history event.
   * @param entity User history event.
   * @return User history table entry.
   */
  private UserHistoryTableEntry genUserHistoryTableEntry(UserHistory entity) {
    // Build manually. Actual code uses mapper.
    return UserHistoryTableEntry.builder()
        .id(entity.getId())
        .createdAt(entity.getCreatedAt())
        .who(entity.getWho())
        .what(entity.getWhat())
        .params(entity.getParams())
        .build();
  }

  //

  /**
   * Create user permission table entries based on user permission entities.
   * @param entities Set of user permission entities.
   * @return List ot user permission table entries.
   */
  public List<UserPermissionTableEntry> genUserPermissionTableEntries(Set<UserPermission> entities) {
    // We need consistent order.
    List<UserPermission> entitiesList = entities.stream().sorted(Comparator.comparing(UserPermission::getId)).toList();
    List<UserPermissionTableEntry> entries = new ArrayList<>();
    for (UserPermission entity : entitiesList) {
      UserPermissionTableEntry entry = genUserPermissionTableEntry(entity);
      entries.add(entry);
    }
    return entries;
  }

  /**
   * Create user permission table entry based on user permission entity.
   * @param entity User permission entity.
   * @return User permission table entry.
   */
  private UserPermissionTableEntry genUserPermissionTableEntry(UserPermission entity) {
    // Build manually. Actual code uses mapper.
    return UserPermissionTableEntry.builder()
        .id(entity.getId())
        .createdAt(entity.getCreatedAt())
        .name(entity.getPermission().getName())
        .value(entity.getValue())
        .build();
  }

  //

  /**
   * Create user token table entries based on user token entities.
   * @param entities List of user token entities.
   * @return List ot user token table entries.
   */
  public List<UserTokenTableEntry> genUserTokenTableEntries(List<UserToken> entities) {
    List<UserTokenTableEntry> entries = new ArrayList<>();
    for (UserToken entity : entities) {
      UserTokenTableEntry entry = genUserTokenTableEntry(entity);
      entries.add(entry);
    }
    return entries;
  }

  /**
   * Create user token table entry based on user token entity.
   * @param entity User token entity.
   * @return User token table entry.
   */
  private UserTokenTableEntry genUserTokenTableEntry(UserToken entity) {
    // Build manually. Actual code uses mapper.
    return UserTokenTableEntry.builder()
        .id(entity.getId())
        .createdAt(entity.getCreatedAt())
        .expiresAt(entity.getExpiresAt())
        .token(entity.getToken())
        .payload(entity.getPayload())
        .build();
  }

  //

  /**
   * Create user JWT table entries based on user JWT entities.
   * @param entities List of user JWT entities.
   * @return List ot user JWT table entries.
   */
  public List<UserJwtTableEntry> genUserJwtTableEntries(Set<UserJwt> entities) {
    // We need consistent order.
    List<UserJwt> entitiesList = entities.stream().sorted(Comparator.comparing(UserJwt::getId)).toList();
    List<UserJwtTableEntry> entries = new ArrayList<>();
    for (UserJwt entity : entitiesList) {
      UserJwtTableEntry entry = genUserJwtTableEntry(entity);
      entries.add(entry);
    }
    return entries;
  }

  /**
   * Create user JWT table entry based on user JWT entity.
   * @param entity User JWT entity.
   * @return User JWT table entry.
   */
  private UserJwtTableEntry genUserJwtTableEntry(UserJwt entity) {
    // Build manually. Actual code uses mapper.
    return UserJwtTableEntry.builder()
        .id(entity.getId())
        .createdAt(entity.getCreatedAt())
        .expiresAt(entity.getExpiresAt())
        .token(entity.getToken())
        .build();
  }
}
