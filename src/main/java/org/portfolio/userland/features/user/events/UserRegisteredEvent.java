package org.portfolio.userland.features.user.events;

/**
 * Event for registering user.
 * @param id User identificator.
 * @param username Username.
 * @param email User email.
 * @param lang User language.
 * @param activationToken Token string.
 * @param activationTokenExpires How long before activation token expires in hours.
 */
public record UserRegisteredEvent(
    Long id,
    String username,
    String email,
    String lang,
    String activationToken,
    Long activationTokenExpires
) {}
