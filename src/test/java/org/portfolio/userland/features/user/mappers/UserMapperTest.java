package org.portfolio.userland.features.user.mappers;

import org.junit.jupiter.api.Test;
import org.portfolio.userland.features.user.dto.admin.config.UserConfigTableEntry;
import org.portfolio.userland.features.user.dto.admin.history.UserHistoryTableEntry;
import org.portfolio.userland.features.user.dto.admin.jwt.UserJwtTableEntry;
import org.portfolio.userland.features.user.dto.admin.permission.UserPermissionTableEntry;
import org.portfolio.userland.features.user.dto.admin.token.UserTokenTableEntry;
import org.portfolio.userland.features.user.dto.admin.user.UserFullDataResp;
import org.portfolio.userland.features.user.dto.admin.user.UserTableEntry;
import org.portfolio.userland.features.user.dto.common.UserDataResp;
import org.portfolio.userland.features.user.dto.common.UserProfileData;
import org.portfolio.userland.features.user.dto.standard.register.UserRegisterReq;
import org.portfolio.userland.features.user.entities.*;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit test for {@link UserMapper}.
 */
public class UserMapperTest {
  private final UserMapper userMapper = new UserMapperImpl();

  // //////////////////////////////////////////////////////////////////////////
  // registerReqToUser

  @Test
  public void registerReqToUser() {
    // Arrange
    UserRegisterReq req = UserRegisterReq.builder()
        .username("Jane")
        .email("jane@test.com")
        .password("secret")
        .lang("en")
        .name("John")
        .surname("Smith")
        .build();

    // Act
    User user = userMapper.registerReqToUser(req);

    // Assert
    assertThat(user.getUsername()).isEqualTo("Jane");
    assertThat(user.getEmail()).isEqualTo("jane@test.com");
    assertThat(user.getLang()).isEqualTo("en");

    // Ignored fields
    assertThat(user.getId()).isNull();
    assertThat(user.getUuid()).isNull();
    assertThat(user.getCreatedAt()).isNull();
    assertThat(user.getModifiedAt()).isNull();
    assertThat(user.getPassword()).isNull(); // password is intentionally ignored
    assertThat(user.getStatus()).isEqualTo(EnUserStatus.PENDING); // ignored by mapper, retains entity default
    assertThat(user.getLocked()).isFalse(); // ignored by mapper, retains entity default
    assertThat(user.getVersion()).isNull();
    assertThat(user.getConfigs()).isEmpty();
    assertThat(user.getHistory()).isEmpty();
    assertThat(user.getTokens()).isEmpty();
    assertThat(user.getJwts()).isEmpty();
    assertThat(user.getPermissions()).isEmpty();
  }

  // //////////////////////////////////////////////////////////////////////////
  // userToDataResp

  @Test
  public void userToDataResp() {
    // Arrange
    User user = new User();
    user.setVersion(3L);
    user.setUsername("Jane");
    user.setEmail("jane@test.com");
    user.setLang("en");

    // Act
    UserDataResp resp = userMapper.userToDataResp(user);

    // Assert
    assertThat(resp.version()).isEqualTo(3L);
    assertThat(resp.username()).isEqualTo("Jane");
    assertThat(resp.email()).isEqualTo("jane@test.com");
    assertThat(resp.lang()).isEqualTo("en");
    assertThat(resp.profile()).isNull(); // ignored
  }

  // //////////////////////////////////////////////////////////////////////////
  // userToFullDataResp

  @Test
  public void userToFullDataResp() {
    // Arrange
    LocalDateTime now = LocalDateTime.of(2026, 4, 10, 10, 0, 0);
    User user = new User();
    user.setId(1L);
    user.setCreatedAt(now);
    user.setModifiedAt(now);
    user.setVersion(5L);
    user.setUsername("Jane");
    user.setEmail("jane@test.com");
    user.setStatus(EnUserStatus.ACTIVE);
    user.setLocked(false);
    user.setLang("en");

    // Act
    UserFullDataResp resp = userMapper.userToFullDataResp(user);

    // Assert
    assertThat(resp.id()).isEqualTo(1L);
    assertThat(resp.createdAt()).isEqualTo(now);
    assertThat(resp.modifiedAt()).isEqualTo(now);
    assertThat(resp.version()).isEqualTo(5L);
    assertThat(resp.username()).isEqualTo("Jane");
    assertThat(resp.email()).isEqualTo("jane@test.com");
    assertThat(resp.status()).isEqualTo(EnUserStatus.ACTIVE);
    assertThat(resp.locked()).isFalse();
    assertThat(resp.lang()).isEqualTo("en");
    assertThat(resp.profile()).isNull(); // ignored
  }

  // //////////////////////////////////////////////////////////////////////////
  // entityToTableEntry(User)

  @Test
  public void entityToTableEntryUser() {
    // Arrange
    LocalDateTime now = LocalDateTime.of(2026, 4, 10, 10, 0, 0);
    User user = new User();
    user.setId(1L);
    user.setCreatedAt(now);
    user.setUsername("Jane");
    user.setEmail("jane@test.com");

    // Act
    UserTableEntry entry = userMapper.entityToTableEntry(user);

    // Assert
    assertThat(entry.id()).isEqualTo(1L);
    assertThat(entry.createdAt()).isEqualTo(now);
    assertThat(entry.username()).isEqualTo("Jane");
    assertThat(entry.email()).isEqualTo("jane@test.com");
  }

  // //////////////////////////////////////////////////////////////////////////
  // profileToData

  @Test
  public void profileToData() {
    // Arrange
    UserProfile profile = new UserProfile();
    profile.setName("John");
    profile.setSurname("Smith");

    // Act
    UserProfileData data = userMapper.profileToData(profile);

    // Assert
    assertThat(data.name()).isEqualTo("John");
    assertThat(data.surname()).isEqualTo("Smith");
  }

  // //////////////////////////////////////////////////////////////////////////
  // entityToTableEntry(UserConfig)

  @Test
  public void entityToTableEntryUserConfig() {
    // Arrange
    LocalDateTime now = LocalDateTime.of(2026, 4, 10, 10, 0, 0);
    UserConfig config = new UserConfig();
    config.setId(1L);
    config.setCreatedAt(now);
    config.setName("jwt.expire");
    config.setValue("60");

    // Act
    UserConfigTableEntry entry = userMapper.entityToTableEntry(config);

    // Assert
    assertThat(entry.id()).isEqualTo(1L);
    assertThat(entry.createdAt()).isEqualTo(now);
    assertThat(entry.name()).isEqualTo("jwt.expire");
    assertThat(entry.value()).isEqualTo("60");
    assertThat(entry.meta()).isNull(); // ignored
  }

  // //////////////////////////////////////////////////////////////////////////
  // entityToTableEntry(UserPermission)

  @Test
  public void entityToTableEntryUserPermission() {
    // Arrange
    LocalDateTime now = LocalDateTime.of(2026, 4, 10, 10, 0, 0);
    Permission permission = new Permission();
    permission.setName("role");

    UserPermission userPermission = new UserPermission();
    userPermission.setId(1L);
    userPermission.setCreatedAt(now);
    userPermission.setValue("operator");
    userPermission.setPermission(permission);

    // Act
    UserPermissionTableEntry entry = userMapper.entityToTableEntry(userPermission);

    // Assert
    assertThat(entry.id()).isEqualTo(1L);
    assertThat(entry.createdAt()).isEqualTo(now);
    assertThat(entry.name()).isEqualTo("role"); // extracted from permission.getName()
    assertThat(entry.value()).isEqualTo("operator");
    assertThat(entry.meta()).isNull(); // ignored
  }

  // //////////////////////////////////////////////////////////////////////////
  // entityToTableEntry(UserHistory)

  @Test
  public void entityToTableEntryUserHistory() {
    // Arrange
    LocalDateTime now = LocalDateTime.of(2026, 4, 10, 10, 0, 0);
    UserHistory history = new UserHistory();
    history.setId(1L);
    history.setCreatedAt(now);
    history.setWho(EnUserHistoryWho.USER);
    history.setWhat(EnUserHistoryWhat.LOGIN);
    history.setParams("IP: '1.2.3.4'");

    // Act
    UserHistoryTableEntry entry = userMapper.entityToTableEntry(history);

    // Assert
    assertThat(entry.id()).isEqualTo(1L);
    assertThat(entry.createdAt()).isEqualTo(now);
    assertThat(entry.who()).isEqualTo(EnUserHistoryWho.USER);
    assertThat(entry.what()).isEqualTo(EnUserHistoryWhat.LOGIN);
    assertThat(entry.params()).isEqualTo("IP: '1.2.3.4'");
  }

  // //////////////////////////////////////////////////////////////////////////
  // entityToTableEntry(UserToken)

  @Test
  public void entityToTableEntryUserToken() {
    // Arrange
    LocalDateTime now = LocalDateTime.of(2026, 4, 10, 10, 0, 0);
    LocalDateTime expires = LocalDateTime.of(2026, 4, 11, 10, 0, 0);
    UserToken token = new UserToken();
    token.setId(1L);
    token.setCreatedAt(now);
    token.setExpiresAt(expires);
    token.setToken("oMVoUQeNa5CRS13dBvMSz1LwaSfFXMfpN");
    token.setPayload("new.email@test.com");

    // Act
    UserTokenTableEntry entry = userMapper.entityToTableEntry(token);

    // Assert
    assertThat(entry.id()).isEqualTo(1L);
    assertThat(entry.createdAt()).isEqualTo(now);
    assertThat(entry.expiresAt()).isEqualTo(expires);
    assertThat(entry.token()).isEqualTo("oMVoUQeNa5..."); // masked
    assertThat(entry.payload()).isEqualTo("new.email@test.com");
  }

  // //////////////////////////////////////////////////////////////////////////
  // entityToTableEntry(UserJwt)

  @Test
  public void entityToTableEntryUserJwt() {
    // Arrange
    LocalDateTime now = LocalDateTime.of(2026, 4, 10, 10, 0, 0);
    LocalDateTime expires = LocalDateTime.of(2026, 4, 11, 10, 0, 0);
    String fullJwt = "eyJhbGciOiJIUzI1NiJ9.eyJyb2xlIjoiYWRtaW4iLCJuYW1lIjoiUGF3ZcWCIFBhcGllcmtvd3NraSIsInN1YiI6InBhd2VsLnBhcGllcmtvd3NraUBnbWFpbC5jb20iLCJpYXQiOjE3ODAwNjgzNzAsImV4cCI6MTc4MDE1NDc3MH0.1SqWUyiexH9WTLt8-LpovCk8UJ74dzUyw_f-Dop4kgA";
    UserJwt jwt = new UserJwt();
    jwt.setId(1L);
    jwt.setCreatedAt(now);
    jwt.setExpiresAt(expires);
    jwt.setToken(fullJwt);

    // Act
    UserJwtTableEntry entry = userMapper.entityToTableEntry(jwt);

    // Assert
    assertThat(entry.id()).isEqualTo(1L);
    assertThat(entry.createdAt()).isEqualTo(now);
    assertThat(entry.expiresAt()).isEqualTo(expires);
    assertThat(entry.token()).isEqualTo("eyJhbGciOi..._f-Dop4kgA"); // middle-abbreviated
  }
}
