package org.portfolio.userland.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

/**
 * Metadata for table presentation. Used in requests that retrieve page from table. All fields can be null, in this case
 * defaults will be used.
 * @param pageSize Size of page.
 * @param page Page number.
 * @param sortBy Name of field to sort by.
 * @param sortOrder Sort order for sortBy.
 */
@Builder(toBuilder = true)
@Schema(description = " Metadata for table presentation. Used in requests that retrieve page from table. All fields can be null, in this case defaults will be used.")
public record TableMeta(
    @Schema(description = "Size of page.", example = "20")
    Integer pageSize,

    @Schema(description = "Page number.", example = "3")
    Integer page,

    @Schema(description = "Name of field to sort by. If null/empty, will sort by default field (usually createdAt).", example = "username")
    String sortBy,

    @Schema(description = "Sort order for sortBy.", example = "DESC")
    EnSortOrder sortOrder
) {}
