package org.portfolio.userland.system.history.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.portfolio.userland.common.annotations.NoCoverageGenerated;
import org.portfolio.userland.features.user.entities.User;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * System history event.
 */
@Entity
@Table(name = "history", schema = "aux")
@Getter
@Setter
public class SystemHistory {
  /** Identificator. */
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** UUID. Acts as business key. */
  @Column(unique = true, nullable = false, updatable = false)
  private String uuid;

  /** User that has this history event. */
  @ManyToOne
  @JoinColumn(name = "id_user")
  private User user;

  //

  /** Date&time of history event creation. */
  @Column(nullable = false, updatable = false)
  private LocalDateTime createdAt;

  //

  /** Who caused history event? */
  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  private EnHistoryWho who;

  /** What caused history event? */
  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  private EnHistoryWhat what;

  /** Event parameters. */
  @Column(nullable = false)
  private String params;

  // //////////////////////////////////////////////////////////////////////////

  @Override
  @NoCoverageGenerated
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    SystemHistory systemHistory = (SystemHistory) o;

    if (uuid == null) return false;
    return Objects.equals(uuid, systemHistory.getUuid());
  }

  @Override
  @NoCoverageGenerated
  public int hashCode() {
    return Objects.hash(uuid);
  }
}
