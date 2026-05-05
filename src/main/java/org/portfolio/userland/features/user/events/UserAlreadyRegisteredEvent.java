package org.portfolio.userland.features.user.events;

import org.portfolio.userland.features.user.dto.common.EnFrontendFramework;

/**
 * Event for trying to register already registered user.
 * @param id User identificator.
 * @param username Username.
 * @param email User email.
 * @param lang User language.
 * @param frontend Frontend.
 */
public record UserAlreadyRegisteredEvent(
    Long id,
    String username,
    String email,
    String lang,
    EnFrontendFramework frontend
) implements BaseUserEvent {}
