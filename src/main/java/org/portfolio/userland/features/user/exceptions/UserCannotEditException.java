package org.portfolio.userland.features.user.exceptions;

import org.portfolio.userland.common.exception.GeneralException;
import org.portfolio.userland.features.user.constants.UserErrCode;
import org.springframework.http.HttpStatus;

/**
 * Thrown when you try to edit user that you are not allowed to edit. Reason might be lack of permissions or trying to
 * edit your own account.
 */
public class UserCannotEditException extends GeneralException {
  private final Long id;

  public UserCannotEditException(Long id) {
    super(id == null ? "" : id.toString());
    this.id = id;
  }

  @Override
  public HttpStatus getStatus() {
    return HttpStatus.CONFLICT;
  }

  @Override
  public String getTitle() {
    return "Not allowed to edit this user.";
  }

  @Override
  public String getDetail() {
    return "User with id '"+id+"' cannot be edited.";
  }

  @Override
  public String getType() {
    return "https://api.userland.org/errors/user/cannotEdit";
  }

  @Override
  public String getErrCode() {
    return UserErrCode.CANNOT_EDIT;
  }
}
