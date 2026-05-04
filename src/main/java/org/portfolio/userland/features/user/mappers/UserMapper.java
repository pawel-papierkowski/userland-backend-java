package org.portfolio.userland.features.user.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.portfolio.userland.features.user.dto.common.UserDataResp;
import org.portfolio.userland.features.user.dto.register.UserRegisterReq;
import org.portfolio.userland.features.user.entities.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.util.HtmlUtils;

/**
 * Maps requests to <code>User</code> and <code>User</code> to responses.
 */
@Mapper(componentModel = "spring", imports = {HtmlUtils.class})
public abstract class UserMapper {
  @Autowired
  protected PasswordEncoder passwordEncoder;

  /**
   * Maps registration request to <code>User</code> entity.
   * <p>Notes:</p>
   * <ul>
   *   <li>Username is sanitized, since it is shown in emails or frontend as is.</li>
   *   <li>Password is hashed properly.</li>
   * </ul>
   * @param req Registration request.
   * @return <code>User</code> entity.
   */
  @Mapping(target = "id", ignore = true)
  @Mapping(target = "uuid", ignore = true)
  @Mapping(target = "createdAt", ignore = true)
  @Mapping(target = "modifiedAt", ignore = true)
  @Mapping(target = "username", expression = "java(HtmlUtils.htmlEscape(req.username()))")
  // email as is
  @Mapping(target = "password", expression = "java(passwordEncoder.encode(req.password()))")
  // lang as is
  @Mapping(target = "status", ignore = true)
  @Mapping(target = "locked", ignore = true)
  @Mapping(target = "configs", ignore = true)
  @Mapping(target = "history", ignore = true)
  @Mapping(target = "tokens", ignore = true)
  @Mapping(target = "jwts", ignore = true)
  @Mapping(target = "permissions", ignore = true)
  public abstract User registerReqToUser(UserRegisterReq req);

  /**
   * Maps <code>User</code> entity to user data response.
   * @param user <code>User</code> entity.
   * @return User data response.
   */
  @Mapping(target = "profile", ignore = true)
  public abstract UserDataResp userToDataResp(User user);
}
