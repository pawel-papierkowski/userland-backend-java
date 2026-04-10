package org.portfolio.userland.features.user.data;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * JPA entity: user.
 */
@Entity
@Table(name = "users") // "user" is a reserved keyword in PostgreSQL, always use "users"
@Getter
@Setter
public class User {
  /** Identificator. */
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** Date&time of account creation. */
  @Column(nullable = false, updatable = false)
  private LocalDateTime createdAt;

  /** Date&time of account last modification. */
  @Column(nullable = false)
  private LocalDateTime modifiedAt;

  //

  /** Name of user visible on frontend. */
  @Column(nullable = false)
  @NotBlank(message = "Username cannot be empty")
  private String username;

  /** E-mail. Also acts as login. Unique and cannot be changed. Serves as business key. */
  @Column(unique = true, nullable = false)
  @NotBlank(message = "Email cannot be empty")
  @Email(regexp = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$", message = "Must be a valid email address")
  private String email;

  /** Password. Note: this will store BCrypt hash, not the plain text. */
  @Column(nullable = false)
  @NotBlank(message = "Password cannot be empty")
  private String password;

  //

  /** Status of user. */
  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  private EnUserStatus status = EnUserStatus.PENDING;

  /** Is user blocked? */
  @Column(nullable = false)
  private Boolean blocked = false;

  //

  @OneToMany(mappedBy = "user")
  private List<UserToken> tokens = new ArrayList<>();

  // //////////////////////////////////////////////////////////////////////////

  @Override
  public boolean equals(Object o) {
    // Of course, I know him. He is me.
    if (this == o) return true;
    // We use getClass() instead of instanceof to handle Hibernate Proxies correctly.
    if (o == null || getClass() != o.getClass()) return false;

    User user = (User) o;

    // If THIS email is null, it's a transient entity and can only equal itself (caught above).
    if (email == null) return false;
    // We only compare the email address, as it is our business key.
    return Objects.equals(email, user.getEmail());
  }

  @Override
  public int hashCode() {
    // We only hash the email address, as it is our business key.
    return Objects.hash(email);
  }
}
