package com.cloudmen.cloudguard.service;

import com.cloudmen.cloudguard.utility.SecurityEmailHtml;
import com.cloudmen.cloudguard.utility.UtilityFunctions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

/**
 * Service responsible for sending administrative notifications regarding user access and onboarding requests. <p>
 *
 * This service alerts CLOUDMEN system administrators when a user requires manual intervention, such as being assigned to an
 * organization or receiving a specific security role. All emails are sent asynchronously to the configured
 * notification recipients.
 */
@Service
public class AccessRequestEmailService {
    private static final Logger log = LoggerFactory.getLogger(AccessRequestEmailService.class);

    private final JavaMailSender mailSender;
    private final MessageSource messageSource;

    @Value("${app.registration.notification-emails:}")
    private List<String> recipientEmails;

    @Value("${spring.mail.username:noreply@cloudguard.com}")
    private String fromEmail;

    @Value("${cloudguard.app.public-url:}")
    private String appPublicUrl;

    public AccessRequestEmailService(JavaMailSender mailSender, @Qualifier("messageSource") MessageSource messageSource) {
        this.mailSender = mailSender;
        this.messageSource = messageSource;
    }

    /**
     * Notifies CLOUDMEN administrators that a user has successfully logged in but is not yet associated with an organization.
     *
     * @param userEmail the email address of the user awaiting organization assignment
     */
    @Async
    public void notifyOrganizationRequest(String userEmail) {
        Locale locale = LocaleContextHolder.getLocale();

        String requestType = "Koppeling aan een organisatie";
        String actionRequired = "Deze gebruiker heeft nog geen organisatie. Wijs een organisatie toe via de Accounts Manager.";
        String subject = "Actie vereist: Organisatie aan gebruiker koppelen in CloudGuard";

        sendEmail(userEmail, requestType, actionRequired, subject, locale);
    }

    /**
     * Notifies CLOUDMEN administrators that a user is awaiting role assignment within the system.
     *
     * @param userEmail the email address of the user awaiting role assignment
     */
    @Async
    public void notifyRoleRequest(String userEmail) {
        Locale locale = LocaleContextHolder.getLocale();
        String requestType = "Toewijzing van een CloudGuard rol";
        String actionRequired = "Deze gebruiker heeft succesvol ingelogd, maar heeft nog geen rol (of staat op Unassigned). Wijs een rol toe via de Accounts Manager.";
        String subject = "Actie vereist: Toewijzen van rol aan gebruiker in CloudGuard";


        sendEmail(userEmail, requestType, actionRequired, subject, locale);
    }

    /**
     * Notifies CLOUDMEN administrators that a user is awaiting access within the system.
     *
     * @param userEmail the email address of the user awaiting access
     */
    @Async
    public void notifyAccessRequest(String userEmail) {
        Locale locale = LocaleContextHolder.getLocale();
        String requestType = messageSource.getMessage(
                "email.access.admin.requestType",
                null,
                "CloudGuard access request",
                locale);
        String actionRequired = messageSource.getMessage(
                "email.access.admin.actionRequired",
                null,
                "This user has signed in successfully but does not yet have access to the platform. Approve the access request via the Accounts Manager.",
                locale);
        String subject = messageSource.getMessage(
                "email.access.subject",
                null,
                "Action required: New CloudGuard access request",
                locale);

        sendEmail(userEmail, requestType, actionRequired, subject, locale);
    }

    @Async
    public void sendAccessRequestConfirmationEmailToUser(String userEmail) {
        Locale locale = LocaleContextHolder.getLocale();

        try {
            String subject = messageSource.getMessage(
                    "email.access.user.subject",
                    null,
                    "CloudGuard: Your access request has been received.",
                    locale);
            String intro = messageSource.getMessage(
                    "email.access.user.intro",
                    null,
                    "Thank you for your request. We have received your CloudGuard access request.",
                    locale);
            String body = messageSource.getMessage(
                    "email.access.user.body",
                    null,
                    "A CLOUDMEN administrator will review your request. You will be notified once your access is approved.",
                    locale);
            String footer = messageSource.getMessage(
                    "email.access.user.footer",
                    null,
                    "This is an automated message from CloudGuard.",
                    locale);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(userEmail);
            helper.setSubject(subject);
            try {
                helper.setFrom(fromEmail, "CloudGuard");
            } catch (Exception e) {
                throw new MessagingException(e.getMessage(), e);
            }

            String plainText = intro + "\n\n" + body;
            String html = SecurityEmailHtml.document(
                    "",
                    true,
                    "<p>" + UtilityFunctions.escapeHtml(intro) + "</p><p>" + UtilityFunctions.escapeHtml(body) + "</p>",
                    UtilityFunctions.escapeHtml(footer)
            );
            helper.setText(plainText, html);
            mailSender.send(message);
            log.info("Bevestigingsmail toegangsaanvraag verstuurd naar {}", userEmail);
        } catch (MessagingException | MailException e) {
            log.error("Fout bij versturen bevestigingsmail naar {}", userEmail, e);
        }
    }

    private void sendEmail(String userEmail, String requestType, String actionRequired, String subject, Locale locale) {
        if (recipientEmails == null || recipientEmails.isEmpty()) {
            log.warn("Geen ontvangst e-mails geconfigureerd voor access requests.");
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(recipientEmails.toArray(new String[0]));

            helper.setSubject(subject);

            try {
                helper.setFrom(fromEmail, "CloudGuard");
            } catch (Exception e) {
                throw new MessagingException(e.getMessage(), e);
            }

            helper.setText(
                    buildPlainText(userEmail, requestType, actionRequired, locale),
                    buildHtmlContent(userEmail, requestType, actionRequired, locale)
            );
            mailSender.send(message);
            log.info("Toegangsverzoek e-mail succesvol verstuurd voor gebruiker: {}", userEmail);
        } catch (MessagingException | MailException e) {
            log.error("Fout bij het versturen van toegangsverzoek e-mail voor {}", userEmail, e);
        }
    }

    private String buildPlainText(String userEmail, String requestType, String actionRequired, Locale locale) {
        String intro = messageSource.getMessage(
                "email.access.admin.plain.intro",
                null,
                "New request in CloudGuard",
                locale);
        String userLabel = messageSource.getMessage(
                "email.access.admin.label.user",
                null,
                "User",
                locale);
        String requestTypeLabel = messageSource.getMessage(
                "email.access.admin.label.requestType",
                null,
                "Request type",
                locale);
        String actionLabel = messageSource.getMessage(
                "email.access.admin.label.actionRequired",
                null,
                "Action required",
                locale);
        String platformUrlLabel = messageSource.getMessage(
                "email.access.admin.label.platformUrl",
                null,
                "Platform URL",
                locale);

        StringBuilder sb = new StringBuilder();
        sb.append(intro).append("\n\n")
                .append(userLabel).append(": ").append(userEmail).append("\n")
                .append(requestTypeLabel).append(": ").append(requestType).append("\n\n")
                .append(actionLabel).append(":\n").append(actionRequired);

        String baseUrl = appPublicUrl != null ? appPublicUrl.trim() : "";
        if (!baseUrl.isEmpty()) {
            String link = baseUrl.endsWith("/") ? baseUrl + "accounts-manager" : baseUrl + "/accounts-manager";
            sb.append("\n\n").append(platformUrlLabel).append(": ").append(link);
        }

        return sb.toString();
    }

    private String buildHtmlContent(String userEmail, String requestType, String actionRequired, Locale locale) {
        String extraCss =
                """
                        .title { font-size: 20px; font-weight: 600; color: %s; margin: 0 0 24px 0; }
                        .card { background: %s; border-radius: 8px; padding: 20px; margin-bottom: 16px; border: 1px solid #e5e7eb; }
                        .label { font-size: 12px; font-weight: 600; color: %s; text-transform: uppercase; letter-spacing: 0.5px; margin-bottom: 4px; }
                        .value { font-size: 15px; color: %s; margin-bottom: 16px; font-weight: 500; }
                        .user-email { color: #0284c7; text-decoration: none; }
                        .action-box { background: #f0f9ff; border-left: 4px solid %s; padding: 16px; border-radius: 4px; margin-top: 8px; white-space: pre-wrap; color: #0369a1; font-size: 14px;}
                        """
                        .formatted(
                                SecurityEmailHtml.FOREGROUND,
                                SecurityEmailHtml.CARD_BG,
                                SecurityEmailHtml.MUTED_TEXT,
                                SecurityEmailHtml.FOREGROUND,
                                SecurityEmailHtml.PRIMARY);

        String baseUrl = appPublicUrl != null ? appPublicUrl.trim() : "";
        String buttonBlock = "";

        if (!baseUrl.isEmpty()) {
            // Voorkom dubbele slashes bij het toevoegen van accounts-manager
            String link = baseUrl.endsWith("/") ? baseUrl + "accounts-manager" : baseUrl + "/accounts-manager";
            String buttonLabel = messageSource.getMessage("email.access.button", null, "Beheer in Accounts Manager", locale);

            buttonBlock = """
                <div style="margin-top: 24px; text-align: center;">
                    <a href="%s" style="display: inline-block; background: %s; color: #ffffff; padding: 12px 24px; border-radius: 6px; text-decoration: none; font-weight: 600;">%s</a>
                </div>
                """.formatted(safeHref(link), SecurityEmailHtml.PRIMARY, UtilityFunctions.escapeHtml(buttonLabel));
        }

        String htmlTitle = messageSource.getMessage(
                "email.access.admin.html.title",
                null,
                "Action required: Account update needed",
                locale);
        String userLabel = messageSource.getMessage(
                "email.access.admin.label.user",
                null,
                "User",
                locale);
        String requestTypeLabel = messageSource.getMessage(
                "email.access.admin.label.requestType",
                null,
                "Request type",
                locale);
        String actionLabel = messageSource.getMessage(
                "email.access.admin.label.actionRequired",
                null,
                "Action required",
                locale);
        String footerText = messageSource.getMessage(
                "email.access.admin.footer",
                null,
                "This is an automatically generated message from the CloudGuard platform.",
                locale);

        String content =
                """
                          <h2 class="title">%s</h2>
                          <div class="card">
                            <div class="label">%s</div>
                            <div class="value"><a href="mailto:%s" class="user-email">%s</a></div>
                            <div class="label">%s</div>
                            <div class="value">%s</div>
                            <div class="label">%s</div>
                            <div class="action-box">%s</div>
                          </div>
                          %s
                        """
                        .formatted(
                                UtilityFunctions.escapeHtml(htmlTitle),
                                UtilityFunctions.escapeHtml(userLabel.toUpperCase(locale)),
                                UtilityFunctions.escapeHtml(userEmail),
                                UtilityFunctions.escapeHtml(userEmail),
                                UtilityFunctions.escapeHtml(requestTypeLabel.toUpperCase(locale)),
                                UtilityFunctions.escapeHtml(requestType),
                                UtilityFunctions.escapeHtml(actionLabel.toUpperCase(locale)),
                                UtilityFunctions.escapeHtml(actionRequired),
                                buttonBlock);

        String footer = UtilityFunctions.escapeHtml(footerText);

        return SecurityEmailHtml.document(extraCss, true, content, footer);
    }

    private static String safeHref(String url) {
        if (url == null) {
            return "";
        }
        return UtilityFunctions.escapeHtml(url).replace(" ", "%20");
    }
}
