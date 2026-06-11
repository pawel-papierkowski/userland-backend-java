package org.portfolio.userland.features.user.dto.admin.permission;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import org.portfolio.userland.common.dto.EntryMetaResp;

import java.time.LocalDateTime;

/**
 * Contains data for one user permission record.
 * @param id Identificator of record.
 * @param createdAt When this record was created?
 * @param name Name of permission variable.
 * @param value Value of permission variable.
 */
@Builder(toBuilder = true)
@Schema(description = "Contains one user permission table record.")
public record UserPermissionTableEntry(
    @Schema(description = "Identificator of record.", example = "13")
    Long id,

    @Schema(description = "When this record was created.", example = "2026-04-21T15:27:17")
    LocalDateTime createdAt,

    @Schema(description = "Name of permission variable.", example = "role")
    String name,

    @Schema(description = "Value of permission variable.", example = "operator")
    String value,

    @Schema(description = "Metadata for this entry. Can be null if no metadata provided. ")
    EntryMetaResp meta
) {}
