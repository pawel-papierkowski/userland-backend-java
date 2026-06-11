package org.portfolio.userland.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.util.Map;

/**
 * Metadata for entry. Mainly for frontend so it knows what to do with it.
 * Example: you can tell it to enable/disable options for entry based on data below.
 *
 * @param options Options for entry.
 * @param data Other metadata.
 */
@Builder(toBuilder = true)
@Schema(description = "Metadata for single entry in response.")
public record EntryMetaResp(
    @Schema(description = "Options as map. Key is option identificator, value is instance of EntryOption.")
    Map<String, EntryOption> options,

    @Schema(description = "Other metadata as key-value pairs.")
    Map<String, String> data
) {}
