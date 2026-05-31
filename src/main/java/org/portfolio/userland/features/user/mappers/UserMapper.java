package org.portfolio.userland.features.user.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.portfolio.userland.features.user.dto.admin.config.UserConfigTableEntry;
import org.portfolio.userland.features.user.dto.admin.history.UserHistoryTableEntry;
import org.portfolio.userland.features.user.dto.admin.jwt.UserJwtTableEntry;
import org.portfolio.userland.features.user.dto.admin.permission.UserPermissionTableEntry;
import org.portfolio.userland.features.user.dto.admin.token.UserTokenTableEntry;
import org.portfolio.userland.features.user.dto.admin.user.UserFullDataResp;
import org.portfolio.userland.features.user.dto.admin.user.UserTableEntry;
import org.portfolio.userland.features.user.dto.common.UserDataResp;
import org.portfolio.userland.features.user.dto.common.UserProfileDataResp;
import org.portfolio.userland.features.user.dto.standard.register.UserRegisterReq;
import org.portfolio.userland.features.user.entities.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.util.HtmlUtils;

/**
 * Maps requests to user-related entities and user-related entities to responses.
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
   * Maps <code>User</code> entity to <code>UserDataResp</code>.
   * @param user <code>User</code> entity.
   * @return <code>UserDataResp</code> instance.
   */
  @Mapping(target = "profile", ignore = true)
  public abstract UserDataResp userToDataResp(User user);

  /**
   * Maps <code>User</code> entity to <code>UserFullDataResp</code>.
   * @param user <code>User</code> entity.
   * @return <code>UserFullDataResp</code> instance.
   */
  @Mapping(target = "profile", ignore = true)
  public abstract UserFullDataResp userToFullDataResp(User user);

  /**
   * Maps <code>User</code> entity to <code>UserTableEntry</code>.
   * @param user <code>User</code> entity.
   * @return <code>UserTableEntry</code> instance.
   */
  public abstract UserTableEntry entityToTableEntry(User user);

  //

  /**
   * Maps <code>UserProfile</code> entity to user profile data response.
   * @param userProfile <code>UserProfile</code> entity.
   * @return User profile data response.
   */
  public abstract UserProfileDataResp profileToDataResp(UserProfile userProfile);

  //

  /**
   * Maps <code>UserConfig</code> entity to <code>UserConfigTableEntry</code>.
   * @param userConfig <code>UserConfig</code> entity.
   * @return <code>UserConfigTableEntry</code> instance.
   */
  public abstract UserConfigTableEntry entityToTableEntry(UserConfig userConfig);

  /**
   * Maps <code>UserHistory</code> entity to <code>UserHistoryTableEntry</code>.
   * @param userHistory <code>UserHistory</code> entity.
   * @return <code>UserHistoryTableEntry</code> instance.
   */
  public abstract UserHistoryTableEntry entityToTableEntry(UserHistory userHistory);

  /**
   * Maps <code>UserJwt</code> entity to <code>UserJwtTableEntry</code>.
   * @param userJwt <code>UserJwt</code> entity.
   * @return <code>UserJwtTableEntry</code> instance.
   */
  public abstract UserJwtTableEntry entityToTableEntry(UserJwt userJwt);

  /**
   * Maps <code>UserToken</code> entity to <code>UserTokenTableEntry</code>.
   * @param userToken <code>UserToken</code> entity.
   * @return <code>UserTokenTableEntry</code> instance.
   */
  public abstract UserTokenTableEntry entityToTableEntry(UserToken userToken);

  /**
   * Maps <code>UserPermission</code> entity to <code>UserPermissionTableEntry</code>.
   * TODO: do something about name field...
   * @param userPermission <code>UserPermission</code> entity.
   * @return <code>UserPermissionTableEntry</code> instance.
   */
  public abstract UserPermissionTableEntry entityToTableEntry(UserPermission userPermission);
}
