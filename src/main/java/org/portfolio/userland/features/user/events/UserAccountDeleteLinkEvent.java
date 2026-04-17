package org.portfolio.userland.features.user.events;

/**
 * Event for account delete send.
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
    String accountDeleteToken,
    long accountDeleteTokenExpires
) {}
