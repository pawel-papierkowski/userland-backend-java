package org.portfolio.userland.system.lockdown.dto;

import lombok.Getter;

/**
 * State of system lockdown.
 */
public enum EnSystemLockdownState {
  /** System lockdown is inactive. Endpoints operate normally. */
  OFF("0"),
  /** System lockdown is active. All endpoints are disabled unless you are admin user. */
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
   * @return Enum or null if string is not known value.
   */
  public static EnSystemLockdownState fromStr(String stateStr) {
    return switch (stateStr) {
      case "0" -> EnSystemLockdownState.OFF;
      case "1" -> EnSystemLockdownState.ON;
      default -> null;
    };
  }
}
