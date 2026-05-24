package org.portfolio.userland.features.user.dto.admin.view;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;

/**
 * DTO for user list result. Shows single page.
 */
@Builder(toBuilder = true)
@Schema(description = "Response that returns page from user table.")
public record UserPageResp(
    @Schema(description = "Page of user records.")
    List<UserTableEntry> users
) {}
