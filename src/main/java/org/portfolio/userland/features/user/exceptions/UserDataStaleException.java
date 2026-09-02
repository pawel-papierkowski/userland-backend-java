package org.portfolio.userland.features.user.exceptions;

import org.portfolio.userland.common.exception.GeneralException;
import org.portfolio.userland.features.user.constants.UserErrCode;
import org.springframework.http.HttpStatus;

/**
 * Thrown when data to be modified is stale, that is optimistic locking version sent by client does not match version
 * of the entity. Data was modified by someone else in the meantime and client has to reload data and try again.
 */
public class UserDataStaleException extends GeneralException {
  private final Long id;

  /**
   * Constructor.
   * @param id Identifier of user.
   * @param expectedVersion Version as sent by client.
   * @param actualVersion Current version of the entity.
   */
  public UserDataStaleException(Long id, Long expectedVersion, Long actualVersion) {
    super("User with id '" + id + "' is stale: expected version " + expectedVersion + ", actual version " + actualVersion + ".");
    this.id = id;
  }

  @Override
  public HttpStatus getStatus() {
    return HttpStatus.CONFLICT;
  }

  @Override
  public String getTitle() {
    return "Data was modified in the meantime.";
  }

  @Override
  public String getDetail() {
    return "User with id '" + id + "' was modified by someone else. Please reload data and try again.";
  }

  @Override
  public String getType() {
    return "https://api.userland.org/errors/user/dataStale";
  }

  @Override
  public String getErrCode() {
    return UserErrCode.DATA_STALE;
  }
}
