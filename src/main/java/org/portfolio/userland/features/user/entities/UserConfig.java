package org.portfolio.userland.features.user.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
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
 * User configuration entry.
 */
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "config", schema = "iam", uniqueConstraints = @UniqueConstraint(columnNames = {"id_user", "name"}))
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
  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "id_user")
  @OnDelete(action = OnDeleteAction.CASCADE)
  private User user;

  //

  /** Date&time of user config entry creation.
   * <p>Maintained automatically by JPA auditing (see <code>org.portfolio.userland.config.JpaAuditingConfig</code>), do not set it manually.</p> */
  @Column(nullable = false, updatable = false)
  @CreatedDate
  private LocalDateTime createdAt;

  //

  /** Name of user configuration entry. Acts as business key. */
  @Column(nullable = false, length = 250)
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
