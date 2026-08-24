package org.portfolio.userland.gcp.constants;

/**
 * GCP-related constants.
 */
public class GcpConst {
  /** Path of the endpoint that actually sends emails (called back by Cloud Tasks). */
  public static final String EMAIL_SEND_ENDPOINT_PATH = "/api/gcp/email/send";
}
