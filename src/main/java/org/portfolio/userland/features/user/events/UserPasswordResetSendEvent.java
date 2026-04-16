package org.portfolio.userland.features.user.events;

/**
 * Event for password reset send.
 * @param id User identificator.
 * @param username Username.
 * @param email User email.
 * @param lang User language.
 * @param passwordResetToken Token string.
 * @param passwordResetTokenExpires How long before password reset token expires in minutes.
 */
public record UserPasswordResetSendEvent(
    Long id,
    String username,
    String email,
    String lang,
    String passwordResetToken,
    long passwordResetTokenExpires
) {}
