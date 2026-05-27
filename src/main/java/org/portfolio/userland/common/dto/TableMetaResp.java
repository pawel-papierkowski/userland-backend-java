package org.portfolio.userland.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

/**
 * Metadata for table response. Used in responses for table page.
 * @param pageCount Count of all pages.
 * @param entryCount Count of all entries (not just on current page).
 * @param pageSize Size of page.
 * @param page Page number.
 * @param sortBy Name of field to sort by.
 * @param sortOrder Sort order for sortBy.
 */
@Builder(toBuilder = true)
@Schema(description = "Metadata for table response. Used in responses for table page.")
public record TableMetaResp(
    @Schema(description = "Count of pages.")
    Long pageCount,
    @Schema(description = "Count of all entries.")
    Long entryCount,

    @Schema(description = "Size of page.", example = "20")
    Integer pageSize,
    @Schema(description = "Page number.", example = "3")
    Integer page,
    @Schema(description = "Name of field to sort by. If null/empty, will sort by default field (usually createdAt).", example = "username")
    String sortBy,
    @Schema(description = "Sort order for sortBy. If null/empty, will use descending order.", example = "ASC")
    EnSortOrder sortOrder
) {}
