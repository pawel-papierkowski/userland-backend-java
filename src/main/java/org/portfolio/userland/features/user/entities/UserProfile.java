package org.portfolio.userland.features.user.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>User profile. User always has profile. Note database does not enforce it, you are responsible for that.</p>
 * <p>Contains business data. Note all of it is optional. This allows user to register account with minimal information.
 * Later user can fill more data if needed for payment or whatever you have.</p>
 * <p>Note: for this particular use having separate 1:1 user profile table is overkill. I decided to do it for
 * demonstration purposes, since it is portfolio project.</p>
 */
@Entity
@Table(name = "profiles", schema = "iam")
@Getter
@Setter
public class UserProfile {
  /** Identificator. Note: no generated value, as we use @MapsId. Profile will always have same id as main user table. */
  @Id
  private Long id;
  /** User entity that owns this profile. Note UserProfile owns this relationship. */
  @OneToOne(optional = false, fetch = FetchType.LAZY)
  @MapsId // Ensures id of UserProfile is same as id of User.
  @JoinColumn(name = "id", unique = true, nullable = false, updatable = false)
  private User user;

  //

  /** Name of user. */
  @Column
  private String name;

  /** Surname of user. */
  @Column
  private String surname;

  // ... you can add other needed data here, like address, invoice data, billing etc
}
