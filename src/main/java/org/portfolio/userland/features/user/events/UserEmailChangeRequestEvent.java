package org.portfolio.userland.features.user.events;

import org.portfolio.userland.features.user.dto.common.EnFrontendFramework;

/**
 * Event for email change request.
 * @param id User identificator.
 * @param username Username.
 * @param email User email.
 * @param lang User language.
 * @param frontend Frontend.
 * @param newEmail New email.
 * @param emailChangeToken Token string.
 * @param emailChangeTokenExpires How long before email change token expires in minutes.
 */
public record UserEmailChangeRequestEvent(
    Long id,
    String username,
    String email,
    String lang,
    EnFrontendFramework frontend,
    String newEmail,
    String emailChangeToken,
    long emailChangeTokenExpires
) implements BaseUserEvent {}
