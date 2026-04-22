package org.portfolio.userland.features.config.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

/**
 * UserLand system configuration. For configuration that should go in effect immediately without restarting the system.
 */
@Entity
@Table(name = "config", schema = "aux")
@Getter
@Setter
public class UlConfig {
  /** Identificator. */
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** Name of configuration variable. Acts as business key. */
  @Column(unique = true, nullable = false)
  @NotBlank(message = "Configuration variable name cannot be empty")
  private String name;

  /** Value of configuration variable. */
  @Column(nullable = false)
  @NotBlank(message = "Configuration variable value cannot be empty")
  private String value;

  /** Description of configuration variable for the developer. */
  @Column(nullable = false)
  @NotBlank(message = "Description cannot be empty")
  private String description;

  // //////////////////////////////////////////////////////////////////////////

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    UlConfig ulConfig = (UlConfig) o;

    if (name == null) return false;
    return Objects.equals(name, ulConfig.getName());
  }

  @Override
  public int hashCode() {
    return Objects.hash(name);
  }
}
