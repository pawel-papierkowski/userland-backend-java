package org.portfolio.userland.features.user.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.portfolio.userland.features.user.data.User;
import org.portfolio.userland.features.user.dto.register.UserRegisterReq;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Maps UserRegisterReq to User.
 * Note: password will be hashed properly.
 */
@Mapper(componentModel = "spring")
public abstract class UserRegisterMapper {
  @Autowired
  protected PasswordEncoder passwordEncoder;

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "modifiedAt", ignore = true)
  @Mapping(target = "password", expression = "java(passwordEncoder.encode(req.password()))")
  @Mapping(target = "status", ignore = true)
  @Mapping(target = "blocked", ignore = true)
  @Mapping(target = "history", ignore = true)
  @Mapping(target = "tokens", ignore = true)
  public abstract User toEntity(UserRegisterReq req);
}
