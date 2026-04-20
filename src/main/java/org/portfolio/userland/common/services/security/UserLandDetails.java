package org.portfolio.userland.common.services.security;

import lombok.Getter;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.portfolio.userland.features.user.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

public class UserLandDetails implements UserDetails {
  @Getter
  private final Long id;
  private final String username;
  @Getter
  private final String email;
  private final String password;
  private final Collection<? extends GrantedAuthority> authorities;

  public UserLandDetails(User user, Collection<? extends GrantedAuthority> authorities) {
    this.id = user.getId();
    this.username = user.getUsername();
    this.email = user.getEmail();
    this.password = user.getPassword(); // Used for login validation, then erased.
    this.authorities = authorities;
  }

  //

  @Override
  public @NonNull String getUsername() {
    return username;
  }

  @Override
  public @Nullable String getPassword() {
    return password;
  }

  @Override
  public @NonNull Collection<? extends GrantedAuthority> getAuthorities() {
    return authorities;
  }
}
