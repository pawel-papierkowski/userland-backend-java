package org.portfolio.userland.features.user.events;

import org.portfolio.userland.features.user.dto.common.EnFrontendFramework;

/**
 * Event for activating user.
 * @param id User identifier.
 * @param username Username.
 * @param email User email.
 * @param lang User language.
 * @param frontend Used frontend.
 */
public record UserActivatedEvent(
    Long id,
    String username,
    String email,
    String lang,
    EnFrontendFramework frontend
) implements BaseUserEvent {}
