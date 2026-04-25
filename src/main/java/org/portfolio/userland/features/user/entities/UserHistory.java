package org.portfolio.userland.features.user.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * User history event.
 */
@Entity
@Table(name = "history", schema = "iam")
@Getter
@Setter
public class UserHistory {
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
  private EnUserHistoryWho who;

  /** What caused history event? */
  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  private EnUserHistoryWhat what;

  // //////////////////////////////////////////////////////////////////////////

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    UserHistory userHistory = (UserHistory) o;

    if (uuid == null) return false;
    return Objects.equals(uuid, userHistory.getUuid());
  }

  @Override
  public int hashCode() {
    return Objects.hash(uuid);
  }
}
