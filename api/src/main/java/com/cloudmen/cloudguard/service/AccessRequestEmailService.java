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

@Service
public class AccessRequestEmailService {
    private static final Logger log = LoggerFactory.getLogger(AccessRequestEmailService.class);

    private final JavaMailSender mailSender;
    private final MessageSource messageSource;

    @Value("${app.feedback.notification-emails:}")
    private List<String> recipientEmails;

    @Value("${spring.mail.username:noreply@cloudguard.com}")
    private String fromEmail;

    public AccessRequestEmailService(JavaMailSender mailSender, @Qualifier("messageSource") MessageSource messageSource) {
        this.mailSender = mailSender;
        this.messageSource = messageSource;
    }

    @Async
    public void notifyOrganizationRequest(String userEmail) {
        Locale locale = LocaleContextHolder.getLocale();

        String requestType = messageSource.getMessage("email.access.type.org", null, "Koppeling aan een organisatie", locale);
        String actionRequired = messageSource.getMessage("email.access.action.org", null, "Deze gebruiker heeft nog geen organisatie. Wijs een organisatie toe via de Accounts Manager.", locale);

        sendEmail(userEmail, requestType, actionRequired, locale);
    }

    @Async
    public void notifyRoleRequest(String userEmail) {
        Locale locale = LocaleContextHolder.getLocale();
        String requestType = messageSource.getMessage("email.access.type.role", null, "Toewijzing van een CloudGuard rol", locale);
        String actionRequired = messageSource.getMessage("email.access.action.role", null, "Deze gebruiker heeft succesvol ingelogd, maar heeft nog geen rol (of staat op Unassigned). Wijs een rol toe via de Accounts Manager.", locale);

        sendEmail(userEmail, requestType, actionRequired, locale);
    }

    private void sendEmail(String userEmail, String requestType, String actionRequired, Locale locale) {
        if (recipientEmails == null || recipientEmails.isEmpty()) {
            log.warn("Geen ontvangst e-mails geconfigureerd voor access requests.");
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(recipientEmails.toArray(new String[0]));

            String subject = messageSource.getMessage("email.access.subject", null, "Actie vereist: Nieuw toegangsverzoek in CloudGuard", locale);
            helper.setSubject(subject);

            try {
                helper.setFrom(fromEmail, "CloudGuard");
            } catch (Exception e) {
                throw new MessagingException(e.getMessage());
            }

            helper.setText(
                    buildPlainText(userEmail, requestType, actionRequired),
                    buildHtmlContent(userEmail, requestType, actionRequired)
            );
            mailSender.send(message);
            log.info("Toegangsverzoek e-mail succesvol verstuurd voor gebruiker: {}", userEmail);
        } catch (MessagingException | MailException e) {
            log.error("Fout bij het versturen van toegangsverzoek e-mail voor {}", userEmail, e);
        }
    }

    private String buildPlainText(String userEmail, String requestType, String actionRequired) {
        return "Nieuw toegangsverzoek in CloudGuard\n\n" +
                "Gebruiker: " + userEmail + "\n" +
                "Type verzoek: " + requestType + "\n\n" +
                "Vereiste actie:\n" + actionRequired;
    }

    private String buildHtmlContent(String userEmail, String requestType, String actionRequired) {
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

        String content =
                """
                          <h2 class="title">Actie vereist: Aanpassing account nodig</h2>
                          <div class="card">
                            <div class="label">GEBRUIKER</div>
                            <div class="value"><a href="mailto:%s" class="user-email">%s</a></div>
                            <div class="label">TYPE AANVRAAG</div>
                            <div class="value">%s</div>
                            <div class="label">VEREISTE ACTIE</div>
                            <div class="action-box">%s</div>
                          </div>
                        """
                        .formatted(
                                UtilityFunctions.escapeHtml(userEmail),
                                UtilityFunctions.escapeHtml(userEmail),
                                UtilityFunctions.escapeHtml(requestType),
                                UtilityFunctions.escapeHtml(actionRequired));

        String footer =
                UtilityFunctions.escapeHtml(
                        "Dit is een automatisch gegenereerd bericht vanuit het CloudGuard platform.");

        return SecurityEmailHtml.document(extraCss, true, content, footer);
    }
}
