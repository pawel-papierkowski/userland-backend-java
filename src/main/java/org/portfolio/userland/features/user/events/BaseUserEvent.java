package org.portfolio.userland.features.user.events;

/**
 * Interface for all user event records. Such records must have at least fields described below.
 */
public interface BaseUserEvent {
  Long id();
  String username();
  String email();
  String lang();
}
