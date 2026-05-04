package org.portfolio.userland.features.user.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.portfolio.userland.common.annotations.NoCoverageGenerated;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * User token entry.
 */
@Entity
@Table(name = "tokens", schema = "iam")
@Getter
@Setter
public class UserToken {
  /** Identificator. */
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** User that has this token entry. */
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

  /** Type of token. */
  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  private EnUserTokenType type;

  /** Value of token. Acts as business key. */
  @Column(unique = true, nullable = false, updatable = false)
  private String token;

  /** Payload of token. Only some types of tokens need payload. */
  @Column
  private String payload;

  // //////////////////////////////////////////////////////////////////////////

  @Override
  @NoCoverageGenerated
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    UserToken userToken = (UserToken) o;

    if (token == null) return false;
    return Objects.equals(token, userToken.getToken());
  }

  @Override
  @NoCoverageGenerated
  public int hashCode() {
    return Objects.hash(token);
  }
}
