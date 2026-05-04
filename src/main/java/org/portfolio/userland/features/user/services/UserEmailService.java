package org.portfolio.userland.features.user.services;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Business logic for changing email. Since it is highly sensitive operation (email acts as the user's login and
 * recovery method), process is somewhat involved:
 * <ul>
 *   <li>On frontend user muse be logged. Option to change email should be on profile edit page or similar.</li>
 *   <li>Request: in payload we require both new email address and current password.</li>
 *   <li>Backend verifies password and if new email is already present. In both cases returns same error to prevent email enumeration attack.</li>
 *   <li>Backend creates token and sends TWO emails: warning for old account and email change confirmation link to the new account.</li>
 *   <li>Link leads to special page on frontend where user can click on button. It calls email change confirmation endpoint on backend.</li>
 *   <li>Backend ensures new email was not created in meantime, updates email of user, deletes token and sends email that confirms email change.</li>
 *   <li>Frontend shows result (success or failure).</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class UserEmailService extends BaseUserService {
}
