package org.portfolio.userland.features.user.data;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * JPA entity: user token.
 */
@Entity
@Table(name = "tokens")
@Getter
@Setter
public class UserToken {
  /** Identificator. */
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

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

  /** Value of token. */
  @Column(unique = true, nullable = false, updatable = false)
  private String token;
}
