package org.portfolio.userland.features.email.dto;

import lombok.Builder;

import java.util.List;
import java.util.Map;

/**
 * Request for sending email. Contains all data needed to send arbitrary email using template or directly with content.
 * @param provider  Use this provider. If empty/null, will use default provider.
 * @param lang Language. Example: "pl". If no language or unknown language, will fall back to "en".
 * @param sender Sender address.
 * @param recipients Recipient address(es).
 * @param recipientsCc Recipient address(es), copy.
 * @param recipientsBcc Recipient address(es), hidden copy.
 * @param replyTo Address of reply. Can be null/empty.
 * @param subject Subject of email.
 * @param template Template to use.
 * @param params Parameters for template.
 * @param messageHtml HTML of email message. If null, system will try to compile template to fill this field.
 */
@Builder(toBuilder = true)
public record EmailReq(
    String provider,
    String lang,

    String sender,
    List<String> recipients,
    List<String> recipientsCc,
    List<String> recipientsBcc,
    String replyTo,

    String subject,
    String template,
    Map<String, Object> params,

    String messageHtml
) {}
