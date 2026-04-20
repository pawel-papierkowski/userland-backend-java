package org.portfolio.userland.features.user.events;

import org.portfolio.userland.features.user.dto.common.EnFrontendFramework;

/**
 * Event for password reset link.
 * @param id User identificator.
 * @param username Username.
 * @param email User email.
 * @param lang User language.
 * @param passwordResetToken Token string.
 * @param passwordResetTokenExpires How long before password reset token expires in minutes.
 */
public record UserPasswordResetLinkEvent(
    Long id,
    String username,
    String email,
    String lang,
    EnFrontendFramework frontend,
    String passwordResetToken,
    long passwordResetTokenExpires
) {}
