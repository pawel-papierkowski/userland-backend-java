package org.portfolio.userland.features.user.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.portfolio.userland.common.annotations.NoCoverageGenerated;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * User JWT entry. Exists because we need ability to revoke them.
 */
@Entity
@Table(name = "jwt", schema = "iam")
@Getter
@Setter
public class UserJwt {
  /** Identificator. */
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** User that has this JWT entry. */
  @ManyToOne
  @JoinColumn(name = "id_user")
  private User user;

  //

  /** Date&time of token creation. */
  @Column(nullable = false, updatable = false)
  private LocalDateTime createdAt;

  /** Date&time of token expiration. */
  @Column(nullable = false, updatable = false)
  private LocalDateTime expiresAt;

  //

  /** Value of token. Acts as business key. */
  @Column(unique = true, nullable = false, updatable = false)
  private String token;

  // //////////////////////////////////////////////////////////////////////////

  @Override
  @NoCoverageGenerated
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    UserJwt userJwt = (UserJwt) o;

    if (token == null) return false;
    return Objects.equals(token, userJwt.getToken());
  }

  @Override
  @NoCoverageGenerated
  public int hashCode() {
    return Objects.hash(token);
  }
}
