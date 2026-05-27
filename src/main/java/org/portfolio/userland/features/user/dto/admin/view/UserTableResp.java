package org.portfolio.userland.features.user.dto.admin.view;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.List;

/**
 * DTO for user list result. Shows single page of entries and related metadata like count of pages.
 */
@Builder(toBuilder = true)
@Schema(description = "Response that returns page from user table.")
public record UserTableResp(
    @Schema(description = "Page of user records.")
    List<UserTableEntry> entries,

    @Schema(description = "Count of pages.")
    Long pageCount,
    @Schema(description = "Count of all entries.")
    Long entryCount
) {}
