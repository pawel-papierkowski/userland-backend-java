package org.portfolio.userland.features.check.data;

import io.swagger.v3.oas.annotations.media.Schema;
import org.portfolio.userland.common.constants.EnAppProfile;

import java.time.LocalDateTime;

/**
 * Contains basic data about this system.
 * @param nowAt Current date and time according to server.
 * @param bootAt When server boot up.
 * @param profile Profile of server.
 */
@Schema(description = "Contains basic data about this system.")
public record CheckInfoResp(
    @Schema(description = "Server date and time.", example = "2026-04-10T10:00:00Z")
    LocalDateTime nowAt,
    @Schema(description = "When server started.", example = "2026-04-10T00:00:00Z")
    LocalDateTime bootAt,
    @Schema(description = "Version of system.", example = "0.9.5")
    String version,
    @Schema(description = "Profile of server.", example = "DEV")
    EnAppProfile profile
) {}
