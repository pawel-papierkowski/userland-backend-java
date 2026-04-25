package org.portfolio.userland.test.helpers.factories.user;

import lombok.RequiredArgsConstructor;
import org.portfolio.userland.features.user.entities.Permission;
import org.portfolio.userland.features.user.entities.User;
import org.portfolio.userland.features.user.entities.UserPermission;
import org.portfolio.userland.test.helpers.factories.BaseFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Generates user permission entry for tests.
 */
@Service
@RequiredArgsConstructor
public class UserPermissionFactory extends BaseFactory {
  /**
   * Generate user permission entry and assign it to user.
   * @param user User.
   * @param permission Permission.
   * @param value Value.
   * @return User permission entry.
   */
  public UserPermission genPermissionEntry(User user, Permission permission, String value) {
    LocalDateTime nowAt = clockService.getNowUTC();

    UserPermission userPermission = new UserPermission();
    userPermission.setPermission(permission);
    userPermission.setUuid(securityGeneratorService.uuid());
    userPermission.setCreatedAt(nowAt);
    userPermission.setValue(value);
    user.addPermission(userPermission);
    return userPermission;
  }
}
