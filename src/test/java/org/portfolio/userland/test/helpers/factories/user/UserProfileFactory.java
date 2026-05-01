package org.portfolio.userland.test.helpers.factories.user;

import lombok.RequiredArgsConstructor;
import org.instancio.Instancio;
import org.portfolio.userland.features.user.entities.User;
import org.portfolio.userland.features.user.entities.UserProfile;
import org.springframework.stereotype.Service;

import static org.instancio.Select.field;

/**
 * Generates user profiles for tests.
 */
@Service
@RequiredArgsConstructor
public class UserProfileFactory {
  /**
   * Generate the simplest profile.
   * @param user User.
   * @return Profile for given user.
   */
  public UserProfile genProfile(User user) {
    UserProfile userProfile = new UserProfile();
    userProfile.setId(user.getId());
    userProfile.setUser(user);
    // all other fields are null
    return userProfile;
  }

  /**
   * Generate the full profile.
   * @param user User.
   * @param name Name of user.
   * @param surname Surname of user.
   * @return Profile for given user.
   */
  public UserProfile genProfile(User user, String name, String surname) {
    UserProfile userProfile = new UserProfile();
    userProfile.setId(user.getId());
    userProfile.setUser(user);
    userProfile.setName(name);
    userProfile.setSurname(surname);
    // all other fields are null
    return userProfile;
  }

  /**
   * Generate the full random profile.
   * @param user User.
   * @return Profile for given user.
   */
  public UserProfile genRandProfile(User user) {
    UserProfile userProfile = Instancio.of(UserProfile.class)
        .set(field(UserProfile::getId), user.getId())
        .set(field(UserProfile::getUser), user)
        .create();
    // all other fields are filled randomly
    return userProfile;
  }
}
