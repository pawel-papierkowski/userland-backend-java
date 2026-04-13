package org.portfolio.userland.common.services.email.data;

import java.util.Map;

/**
 * Request for email sending.
 */
public record EmailReq(
    String provider, // Use this provider. If empty/null, will use default provider.

    String sender, // sender address
    String[] recipients, // recipient address(es)
    String[] recipientsCc, // Recipient address(es), copy.
    String[] recipientsBcc, // Recipient address(es), hidden copy.
    String replyTo, // Address of reply. Can be null/empty.

    String subject, // Title of email.
    String template, // Template to use.
    Map<String, String> params, // Parameters for template.

    String messageHtml // HTML of email message. If null, will try to compile template to fill this field.
) {}
