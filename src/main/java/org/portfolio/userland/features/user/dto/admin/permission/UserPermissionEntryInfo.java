package org.portfolio.userland.features.user.dto.admin.permission;

/**
 * Lightweight projection of user permission entry, used when only basic info about the entry is needed
 * (like for history messages), so we avoid hydrating full {@code UserPermission} and {@code Permission} entities.
 * @param userId Identifier of user owning this entry.
 * @param permissionName Name of the permission of this entry.
 * @param value Value of this entry.
 */
public record UserPermissionEntryInfo(
    Long userId,
    String permissionName,
    String value
) {}
