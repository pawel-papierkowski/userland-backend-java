package org.portfolio.userland.features.user.events;

import org.portfolio.userland.features.user.dto.common.EnFrontendFramework;

/**
 * Event for account delete link.
 * @param id User identificator.
 * @param username Username.
 * @param email User email.
 * @param lang User language.
 * @param accountDeleteToken Token string.
 * @param accountDeleteTokenExpires How long before password reset token expires in minutes.
 */
public record UserAccountDeleteLinkEvent(
    Long id,
    String username,
    String email,
    String lang,
    EnFrontendFramework frontend,
    String accountDeleteToken,
    long accountDeleteTokenExpires
) {}
