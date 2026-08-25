package org.portfolio.userland.gcp.services;

/**
 * Abstraction over GCP Cloud Tasks enqueueing. Decouples business services from the concrete Cloud Tasks client,
 * so no null-checks or profile-awareness is needed at call sites:
 * <ul>
 *   <li>{@link GcpTaskEnqueuer} - real implementation, active on 'gcp' profile.</li>
 *   <li>{@link NoopTaskEnqueuer} - does nothing, active everywhere else (local development, tests).</li>
 * </ul>
 */
public interface TaskEnqueuer {
  /**
   * Enqueue task that will asynchronously call back given endpoint of this service with given payload.
   * @param queueId Id of the queue to enqueue into (without project/location prefix).
   * @param endpointPath Path of the endpoint that will be called back (relative to {@code app.gcp.general.url}).
   * @param payload Payload object; will be serialized to JSON as task body.
   */
  void enqueue(String queueId, String endpointPath, Object payload);
}
