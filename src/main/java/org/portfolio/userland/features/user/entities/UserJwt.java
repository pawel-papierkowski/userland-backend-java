package org.portfolio.userland.features.user.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.portfolio.userland.common.annotations.NoCoverageGenerated;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * User JWT entry. Exists because we need ability to revoke them.
 */
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "jwt", schema = "iam")
@Getter
@Setter
public class UserJwt {
  /** Identificator. */
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** User that has this JWT entry. */
  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "id_user")
  @OnDelete(action = OnDeleteAction.CASCADE)
  private User user;

  //

  /** Date&time of token creation.
   * <p>Maintained automatically by JPA auditing (see <code>org.portfolio.userland.config.JpaAuditingConfig</code>), do not set it manually.</p> */
  @Column(nullable = false, updatable = false)
  @CreatedDate
  private LocalDateTime createdAt;

  /** Date&time of token expiration. */
  @Column(nullable = false, updatable = false)
  private LocalDateTime expiresAt;

  //

  /** Value of token. Acts as business key. */
  @Column(unique = true, nullable = false, updatable = false, length = 255)
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
