package org.portfolio.userland.features.user.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.portfolio.userland.features.user.dto.register.UserRegisterReq;
import org.portfolio.userland.features.user.entities.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.util.HtmlUtils;

/**
 * Maps UserRegisterReq to User.
 * <p>Notes:</p>
 * <ul>
 *   <li>Username is sanitized, as it is shown in emails or on frontend as is.</li>
 *   <li>Password is hashed properly.</li>
 * </ul>
 */
@Mapper(componentModel = "spring", imports = {HtmlUtils.class})
public abstract class UserRegisterMapper {
  @Autowired
  protected PasswordEncoder passwordEncoder;

  @Mapping(target = "id", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "modifiedAt", ignore = true)
  @Mapping(target = "username", expression = "java(HtmlUtils.htmlEscape(req.username()))")
  // email as is
  @Mapping(target = "password", expression = "java(passwordEncoder.encode(req.password()))")
  // lang as is
  @Mapping(target = "status", ignore = true)
  @Mapping(target = "locked", ignore = true)
  @Mapping(target = "history", ignore = true)
  @Mapping(target = "tokens", ignore = true)
  @Mapping(target = "jwts", ignore = true)
  @Mapping(target = "permissions", ignore = true)
  public abstract User toEntity(UserRegisterReq req);
}
