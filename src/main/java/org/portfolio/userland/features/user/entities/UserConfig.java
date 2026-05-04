package org.portfolio.userland.features.user.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.portfolio.userland.common.annotations.NoCoverageGenerated;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * User configuration entry.
 * TODO: no suitable business key exist, what to do? Name is not enough and I really would like to avoid uuid.
 */
@Entity
@Table(name = "config", schema = "iam")
@Getter
@Setter
public class UserConfig {
  /** Identificator. */
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** UUID v4. Acts as business key. */
  @Column(unique = true, nullable = false, updatable = false)
  private UUID uuid;

  /** User that has this configuration entry. */
  @ManyToOne
  @JoinColumn(name = "id_user")
  private User user;

  //

  /** Date&time of user config entry creation. */
  @Column(nullable = false, updatable = false)
  private LocalDateTime createdAt;

  //

  /** Name of user configuration entry. Acts as business key. */
  @Column(nullable = false)
  @NotBlank(message = "Name cannot be empty")
  private String name;

  /** Value of user configuration entry. */
  @Column(nullable = false)
  @NotBlank(message = "Value cannot be empty")
  private String value;

  // //////////////////////////////////////////////////////////////////////////

  @Override
  @NoCoverageGenerated
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    UserConfig userConfig = (UserConfig) o;

    if (uuid == null) return false;
    return Objects.equals(uuid, userConfig.getUuid());
  }

  @Override
  @NoCoverageGenerated
  public int hashCode() {
    return Objects.hash(uuid);
  }
}
