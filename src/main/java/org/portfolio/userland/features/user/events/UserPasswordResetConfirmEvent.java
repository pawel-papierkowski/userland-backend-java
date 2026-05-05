package org.portfolio.userland.features.user.events;

/**
 * Event for password reset confirmation.
 * @param id User identificator.
 * @param username Username.
 * @param email User email.
 * @param lang User language.
 */
public record UserPasswordResetConfirmEvent(
    Long id,
    String username,
    String email,
    String lang
) implements BaseUserEvent {}
