package org.portfolio.userland.features.check.data;

import io.swagger.v3.oas.annotations.media.Schema;
import org.portfolio.userland.common.constants.EnAppProfile;

import java.time.LocalDateTime;

/**
 * Contains basic data about this system.
 * @param nowAt Server date and time.
 * @param profile Profile of server.
 */
@Schema(description = "Contains basic data about this system.")
public record CheckInfoResp(
    @Schema(description = "Server date and time.", example = "2026-04-10T10:00:00Z")
    LocalDateTime nowAt,
    @Schema(description = "Profile of server.", example = "DEV")
    EnAppProfile profile
) {}
