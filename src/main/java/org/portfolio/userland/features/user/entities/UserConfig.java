package org.portfolio.userland.features.user.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.portfolio.userland.common.annotations.NoCoverageGenerated;

import java.util.Objects;

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

  /** User that has this configuration entry. */
  @ManyToOne
  @JoinColumn(name = "id_user")
  private User user;

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

    if (name == null) return false;
    return Objects.equals(name, userConfig.getName());
  }

  @Override
  @NoCoverageGenerated
  public int hashCode() {
    return Objects.hash(name);
  }
}
