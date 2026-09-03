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
import java.util.UUID;

/**
 * User history event.
 */
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "history", schema = "iam")
@Getter
@Setter
public class UserHistory {
  /** Identifier. */
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** UUID v4. Acts as business key. */
  @Column(unique = true, nullable = false, updatable = false)
  private UUID uuid;

  /** User that has this history event. */
  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "id_user")
  @OnDelete(action = OnDeleteAction.CASCADE)
  private User user;

  //

  /** Date&time of history event creation.
   * <p>Maintained automatically by JPA auditing (see <code>org.portfolio.userland.config.JpaAuditingConfig</code>), do not set it manually.</p> */
  @Column(nullable = false, updatable = false)
  @CreatedDate
  private LocalDateTime createdAt;

  //

  /** Who caused history event? */
  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  private EnUserHistoryWho who;

  /** What history event it is? */
  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  private EnUserHistoryWhat what;

  /** Event parameters. */
  @Column(nullable = false)
  private String params;

  // //////////////////////////////////////////////////////////////////////////

  @Override
  @NoCoverageGenerated
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    UserHistory userHistory = (UserHistory) o;

    if (uuid == null) return false;
    return Objects.equals(uuid, userHistory.getUuid());
  }

  @Override
  @NoCoverageGenerated
  public int hashCode() {
    return Objects.hash(uuid);
  }
}
