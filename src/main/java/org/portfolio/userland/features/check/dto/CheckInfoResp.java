package org.portfolio.userland.features.check.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import org.portfolio.userland.common.constants.EnAppBuild;

import java.time.LocalDateTime;

/**
 * Contains basic data about this system.
 * @param name Name of project.
 * @param nowAt Current date and time according to server (UTC).
 * @param bootAt When server boot up (UTC).
 * @param version Version of project.
 * @param profile Server profile.
 */
@Builder(toBuilder = true)
@Schema(description = "Contains basic data about this system.")
public record CheckInfoResp(
    @Schema(description = "Name of system.", example = "UserLand")
    String name,
    @Schema(description = "Server date and time.", example = "2026-04-10T10:00:00Z")
    LocalDateTime nowAt,
    @Schema(description = "When server started.", example = "2026-04-10T00:00:00Z")
    LocalDateTime bootAt,
    @Schema(description = "Version of system.", example = "0.9.5")
    String version,
    @Schema(description = "Profile of server.", example = "DEV")
    EnAppBuild profile
) {}
