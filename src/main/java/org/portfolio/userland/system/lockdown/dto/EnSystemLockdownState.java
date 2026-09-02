package org.portfolio.userland.system.lockdown.dto;

import lombok.Getter;
import org.portfolio.userland.common.exception.ShouldNeverHappenException;

/**
 * State of system lockdown.
 * If lockdown is active, all endpoints are disabled. Exceptions:
 * <ul>
 *   <li>you are admin/operator user</li>
 *   <li>login endpoint is available, but only admin/operator user can successfully log in</li>
 *   <li>GCP endpoint is exempt in general</li>
 * </ul>
 */
public enum EnSystemLockdownState {
  /** System lockdown is inactive. */
  OFF("0"),
  /** System lockdown is active.  */
  ON("1");

  /** Value of enum as string. */
  @Getter
  private final String value;

  /**
   * Constructor.
   * @param value Value of enum as string.
   */
  EnSystemLockdownState(String value) {
    this.value = value;
  }

  /**
   * Reads string as system lockdown state enum.
   * @param stateStr String.
   * @return Enum corresponding to given string.
   */
  public static EnSystemLockdownState fromStr(String stateStr) {
    return switch (stateStr) {
      case "0" -> EnSystemLockdownState.OFF;
      case "1" -> EnSystemLockdownState.ON;
      default -> throw new ShouldNeverHappenException("'"+stateStr+"' is not known value for EnSystemLockdownState.");
    };
  }
}
