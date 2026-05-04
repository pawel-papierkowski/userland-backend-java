package org.portfolio.userland.features.user.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.portfolio.userland.common.annotations.NoCoverageGenerated;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Many-to-many table describing user permissions.
 * Note: it is not pure many-to-many (we need value of permission, also we remember when that permission was given),
 * so we cannot use @ManyToMany.
 */
@Entity
@Table(name = "user_permissions", schema = "iam")
@Getter
@Setter
public class UserPermission {
  /** Identificator. */
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** UUID v4. Acts as business key. */
  @Column(unique = true, nullable = false, updatable = false)
  private UUID uuid;

  /** User that has this user permission entry. */
  @ManyToOne
  @JoinColumn(name = "id_user")
  private User user;

  /** Permission that has this user permission entry. */
  @ManyToOne
  @JoinColumn(name = "id_permission")
  private Permission permission;

  //

  /** Date&time of user permission entry creation. */
  @Column(nullable = false, updatable = false)
  private LocalDateTime createdAt;

  //

  /** Value of permission entry. */
  @Column(nullable = false)
  private String value;

  // //////////////////////////////////////////////////////////////////////////

  @Override
  @NoCoverageGenerated
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    UserPermission userPermission = (UserPermission) o;

    if (uuid == null) return false;
    return Objects.equals(uuid, userPermission.getUuid());
  }

  @Override
  @NoCoverageGenerated
  public int hashCode() {
    return Objects.hash(uuid);
  }
}
