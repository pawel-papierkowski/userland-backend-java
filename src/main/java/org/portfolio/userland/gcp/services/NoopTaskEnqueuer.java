package org.portfolio.userland.gcp.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * No-op implementation of {@link TaskEnqueuer} for local development and tests. Does nothing, but callers stay
 * safe - there is no need to check environment or guard against missing GCP client. In this way you do not have to
 * install and configure GCloud CLI locally.
 */
@Service
@Profile("!gcp")
@Slf4j
public class NoopTaskEnqueuer implements TaskEnqueuer {
  /**
   * Pretend to enqueue task. Nothing is actually sent anywhere.
   */
  @Override
  public void enqueue(String queueId, String endpointPath, Object payload) {
    log.debug("enqueue(): Skipped - not running on GCP (queue '{}', endpoint '{}').", queueId, endpointPath);
  }
}
