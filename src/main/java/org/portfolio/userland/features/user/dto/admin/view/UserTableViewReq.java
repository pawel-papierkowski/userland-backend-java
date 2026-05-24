package org.portfolio.userland.features.user.dto.admin.view;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import org.portfolio.userland.common.dto.TableMeta;
import org.portfolio.userland.features.user.entities.EnUserStatus;

import java.time.LocalDateTime;

/**
 * DTO for viewing user table. Contains filters and other settings needed for desired result. All fields can be null
 * or empty, in this case defaults will be used.
 * @param username If present, filter by partial username.
 * @param email If present, filter by partial user email.
 * @param status If present, filter by status.
 * @param locked If present, filter by locked.
 * @param createdFromAt If present, show users created at this date or later.
 * @param createdToAt If present, show users created at this date or earlier.
 * @param tableMeta Table metadata like pagination settings or sorting.
 */
@Builder(toBuilder = true)
@Schema(description = "Payload required to return page of results from user table. All fields can be null/empty, defaults will be used.")
public record UserTableViewReq(
    @Schema(description = "If present, will show only records that contain fully or partially this username.", example = "John Doe")
    String username,

    @Schema(description = "If present, will show only records that contain fully or partially this email.", example = "john.doe@example.com")
    String email,

    @Schema(description = "If present, will show only records of users with given status.", example = "ACTIVE")
    EnUserStatus status,

    @Schema(description = "If present, will show only records of users with given locked value.", example = "false")
    Boolean locked,

    @Schema(description = "Show records with creation date that is same or later.", example = "2026-04-01T12:00:00")
    LocalDateTime createdFromAt,

    @Schema(description = "Show records with creation date that is same or earlier.", example = "2026-05-24T12:00:00")
    LocalDateTime createdToAt,

    @Schema(description = "Metadata for table result.")
    TableMeta tableMeta
) {}
