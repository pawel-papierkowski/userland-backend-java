package org.portfolio.userland.common.services.email.data;

import java.util.Map;

/**
 * Request for email sending.
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
public record EmailReq(
    String provider,
    String lang,

    String sender,
    String[] recipients,
    String[] recipientsCc,
    String[] recipientsBcc,
    String replyTo,

    String subject,
    String template,
    Map<String, Object> params,

    String messageHtml
) {
  public EmailReq withMessageHtml(String messageHtml) {
    return new EmailReq(
        this.provider,
        this.lang,
        this.sender,
        this.recipients,
        this.recipientsCc,
        this.recipientsBcc,
        this.replyTo,
        this.subject,
        this.template,
        this.params,
        messageHtml
    );
  }
}
