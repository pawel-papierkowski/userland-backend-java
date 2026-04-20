package org.portfolio.userland.features.user.events;

import org.portfolio.userland.features.user.dto.common.EnFrontendFramework;

/**
 * Event for activating user.
 * @param id User identificator.
 * @param username Username.
 * @param email User email.
 * @param lang User language.
 */
public record UserActivatedEvent(
    Long id,
    String username,
    String email,
    String lang,
    EnFrontendFramework frontend
) {}
