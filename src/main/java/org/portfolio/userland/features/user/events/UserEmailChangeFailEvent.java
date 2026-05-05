package org.portfolio.userland.features.user.events;

import org.portfolio.userland.features.user.dto.common.EnFrontendFramework;

/**
 * Event for email change failure request.
 * @param id User identificator.
 * @param username Username.
 * @param email User email.
 * @param lang User language.
 * @param frontend Frontend.
 * @param newEmail New email.
 */
public record UserEmailChangeFailEvent(
    Long id,
    String username,
    String email,
    String lang,
    EnFrontendFramework frontend,
    String newEmail
) implements BaseUserEvent {}
