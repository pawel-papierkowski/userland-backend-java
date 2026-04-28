package org.portfolio.userland.features.user.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.portfolio.userland.common.annotations.NoCoverageGenerated;

import java.util.Objects;

/**
 * Permissions available for users.
 */
@Entity
@Table(name = "permissions", schema = "iam")
@Getter
@Setter
public class Permission {
  /** Identificator. */
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** Name of permission. Acts as business key. */
  @Column(unique = true, nullable = false)
  private String name;

  /** Indicates if that permission should be embedded in JWT key. */
  @Column(nullable = false)
  private Boolean inJwt;

  /** Indicates if that permission should be included in Spring authorities. */
  @Column(nullable = false)
  private Boolean inAuthorities;

  // //////////////////////////////////////////////////////////////////////////

  @Override
  @NoCoverageGenerated
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Permission permission = (Permission) o;

    if (name == null) return false;
    return Objects.equals(name, permission.getName());
  }

  @Override
  @NoCoverageGenerated
  public int hashCode() {
    return Objects.hash(name);
  }
}
