package org.portfolio.userland.gcp.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.tasks.v2.CloudTasksClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

public abstract class BaseGcpService {
  /** CloudTasksClient is not injected in local development environment, as GCP-related stuff is not used. */
  @Autowired(required = false)
  protected CloudTasksClient cloudTasksClient;
  protected final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

  @Value("${app.gcp.general.url}")
  protected String serviceUrl;
  @Value("${app.gcp.general.service-account}")
  protected String serviceAccount;

  @Value("${app.gcp.general.project}")
  protected String projectId;
  @Value("${app.gcp.general.location}")
  protected String locationId;
  @Value("${app.gcp.email.queue}")
  protected String queueId;
}
