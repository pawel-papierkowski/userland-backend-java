package org.portfolio.userland.features.user.services;

/**
 * Event for registering user.
 * @param id User identificator.
 * @param username Username.
 * @param email User email.
 * @param lang User language.
 * @param activationToken Token string.
 */
public record UserRegisteredEvent(
    Long id,
    String username,
    String email,
    String lang,
    String activationToken
) {}
