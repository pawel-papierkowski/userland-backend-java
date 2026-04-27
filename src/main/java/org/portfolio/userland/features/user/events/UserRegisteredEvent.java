package org.portfolio.userland.features.user.events;

import org.portfolio.userland.features.user.dto.common.EnFrontendFramework;

/**
 * Event for registering user.
 * @param id User identificator.
 * @param username Username.
 * @param email User email.
 * @param lang User language.
 * @param activationToken Token string.
 * @param activationTokenExpires How long before activate token expires in hours.
 */
public record UserRegisteredEvent(
    Long id,
    String username,
    String email,
    String lang,
    EnFrontendFramework frontend,
    String activationToken,
    Long activationTokenExpires
) {}
