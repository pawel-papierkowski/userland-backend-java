package org.portfolio.userland.system.history.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

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

  /** Value of event. */
  @Column(nullable = false)
  private String value;

  // //////////////////////////////////////////////////////////////////////////

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    SystemHistory systemHistory = (SystemHistory) o;

    if (uuid == null) return false;
    return Objects.equals(uuid, systemHistory.getUuid());
  }

  @Override
  public int hashCode() {
    return Objects.hash(uuid);
  }
}
