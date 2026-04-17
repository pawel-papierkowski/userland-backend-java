package org.portfolio.userland.features.user.events;

/**
 * Event for account delete confirmation.
 * @param id User identificator.
 * @param username Username.
 * @param email User email.
 * @param lang User language.
 */
public record UserAccountDeleteConfirmEvent(
    Long id,
    String username,
    String email,
    String lang
) {}
