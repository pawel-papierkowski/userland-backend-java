package org.portfolio.userland.features.user.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.portfolio.userland.common.annotations.NoCoverageGenerated;
import org.portfolio.userland.common.constants.ValidConst;

import java.time.LocalDateTime;
import java.util.*;

/**
 * <p>User account. Contains technical data.</p>
 * <p>Note: you need to handle <code>UserProfile</code> separately. Shouldn't be hard, as you have guarantee that
 * <code>UserProfile</code> for given <code>User</code> has exactly same id as <code>User</code>. It is done in this way
 * to ensure we do not load/use/save profile unnecessarily.</p>
 * <p>To load user profile:</p>
 * <pre>
 *   userProfile = userProfileRepository.findById(user.getId()).orElseThrow();
 * </pre>
 */
@Entity
@Table(name = "users", schema = "iam") // "user" is a reserved keyword in PostgreSQL, always use "users"
@Getter
@Setter
public class User {
  /** Identificator. */
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** Date&time of account creation. */
  @Column(nullable = false, updatable = false)
  private LocalDateTime createdAt;

  /** Date&time of account last modification. */
  @Column(nullable = false)
  private LocalDateTime modifiedAt;

  // basic data

  /** Name of user (can be also nickname) visible on frontend, shown to other uses, in email etc. */
  @Column(nullable = false)
  @NotBlank(message = "Username cannot be empty")
  private String username;

  /** E-mail. Also acts as login. Unique and cannot be changed. Serves as business key. */
  @Column(unique = true, nullable = false, updatable = false)
  @NotBlank(message = "Email cannot be empty")
  @Email(regexp = ValidConst.REG_EXPR_EMAIL, message = "Must be a valid email address")
  private String email;

  /** Password. Note: this stores BCrypt hash, not the plain text. */
  @Column(nullable = false)
  @NotBlank(message = "Password cannot be empty")
  private String password;

  // state

  /** Status of user. */
  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  private EnUserStatus status = EnUserStatus.PENDING;

  /** Is user locked? That means user cannot log in or do anything on this account. */
  @Column(nullable = false)
  private Boolean locked = false;

  // options

  /** Used language. If empty/unknown, will fall back to English. */
  @Column(nullable = false)
  @NotBlank(message = "Language cannot be empty")
  private String lang;

  // related tables (note profile is missing)

  /** History of this user. */
  @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<UserHistory> history = new ArrayList<>();

  /** Tokens that this user has. Same user can have only one token of given type at once. */
  @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<UserToken> tokens = new ArrayList<>();

  /** JWT assigned to this user. */
  @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<UserJwt> jwts = new HashSet<>();

  /** Permissions that this user has. */
  @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
  private Set<UserPermission> permissions = new HashSet<>();

  // //////////////////////////////////////////////////////////////////////////

  /**
   * Add history event to list of history events. Call only if you expect to use history, or it was already used.
   * @param historyEvent History event to add.
   */
  public void addHistory(UserHistory historyEvent) {
    if (history == null) this.history = new ArrayList<>();
    history.add(historyEvent);
    historyEvent.setUser(this);
  }

  /**
   * Add token entry to list of token entries. Call only if you expect to use tokens, or it was already used.
   * @param tokenEntry Token entry to add.
   */
  public void addToken(UserToken tokenEntry) {
    if (tokens == null) this.tokens = new ArrayList<>();
    tokens.add(tokenEntry);
    tokenEntry.setUser(this);
  }

  /**
   * Add JWT entry to list of JWT entries. Call only if you expect to use JWTs, or it was already used.
   * @param jwtEntry JWT entry to add.
   */
  public void addJwt(UserJwt jwtEntry) {
    if (jwts == null) this.jwts = new HashSet<>();
    jwts.add(jwtEntry);
    jwtEntry.setUser(this);
  }

  /**
   * Add permission entry to list of permission entries. Call only if you expect to use permissions, or it was already used.
   * @param permissionEntry Permission entry to add.
   */
  public void addPermission(UserPermission permissionEntry) {
    if (permissions == null) this.permissions = new HashSet<>();
    permissions.add(permissionEntry);
    permissionEntry.setUser(this);
  }

  // //////////////////////////////////////////////////////////////////////////

  @Override
  @NoCoverageGenerated
  public boolean equals(Object o) {
    // Of course, I know him. He is me.
    if (this == o) return true;
    // We use getClass() instead of instanceof to handle Hibernate Proxies correctly.
    if (o == null || getClass() != o.getClass()) return false;

    User user = (User) o;

    // If THIS email is null, it's a transient entity and can only equal itself (caught above).
    if (email == null) return false;
    // We only compare the email address, as it is our business key.
    return Objects.equals(email, user.getEmail());
  }

  @Override
  @NoCoverageGenerated
  public int hashCode() {
    // We only hash the email address, as it is our business key.
    return Objects.hash(email);
  }
}
