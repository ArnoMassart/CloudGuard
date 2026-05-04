package com.cloudmen.cloudguard.service;

import com.cloudmen.cloudguard.utility.SecurityEmailHtml;
import com.cloudmen.cloudguard.utility.UtilityFunctions;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Service
public class ContactEmailService {
    private static final Logger log = LoggerFactory.getLogger(ContactEmailService.class);
    private final MessageSource messageSource;
    private final JavaMailSenderImpl mailSender;

    @Value("${app.feedback.notification-emails}")
    private List<String> recipientEmails;

    @Value("${spring.mail.username:noreply@cloudguard.com}")
    private String fromEmail;

    @Value("${cloudguard.app.public-url:}")
    private String appPublicUrl;

    public ContactEmailService(@Qualifier("messageSource") MessageSource messageSource, JavaMailSenderImpl mailSender) {
        this.messageSource = messageSource;
        this.mailSender = mailSender;
    }

    /**
     * Sends a contact email to the support team based on user input from the frontend.
     *
     * @param userEmail the email address of the user who submitted the form
     * @param topic     the general category of the message (e.g., support, billing, feedback)
     * @param subject   the specific subject line entered by the user
     * @param message   the full message body entered by the user
     */
    @Async
    public void sendContactEmail(String userEmail, String topic, String subject, String message) {
        Locale locale = LocaleContextHolder.getLocale();

        if (recipientEmails == null || recipientEmails.isEmpty()) {
            log.warn("Geen ontvangst e-mails geconfigureerd voor contactberichten.");
            return;
        }

        String readableTopic = getReadableTopic(topic, locale);

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setTo(recipientEmails.toArray(new String[0]));

            // Voorbeeld onderwerp: "Nieuw bericht (Support): Kan niet inloggen"
            String emailSubject = messageSource.getMessage("email.contact.subject",
                    new Object[]{readableTopic, subject},
                    "Nieuw bericht (" + readableTopic + "): " + subject,
                    locale);

            helper.setSubject(emailSubject);

            try {
                helper.setFrom(fromEmail, "CloudGuard Contact");
                // Zet de Reply-To naar de gebruiker, zodat support direct op de mail kan reageren!
                helper.setReplyTo(userEmail);
            } catch (Exception e) {
                throw new MessagingException(e.getMessage());
            }

            helper.setText(
                    buildPlainText(userEmail, readableTopic, subject, message),
                    buildHtmlContent(userEmail, readableTopic, subject, message, locale)
            );

            mailSender.send(mimeMessage);
            log.info("Contactbericht succesvol verstuurd namens gebruiker: {}", userEmail);

        } catch (MessagingException | MailException e) {
           log.error("Fout bij het versturen van contactbericht namens {}", userEmail, e);
        }
    }

    private String getReadableTopic(String rawTopic, Locale locale) {
        if (rawTopic == null) return "Overig";

        return switch (rawTopic.toLowerCase()) {
            case "support" -> messageSource.getMessage("contact.topic.support", null, "Technische ondersteuning", locale);
            case "account" -> messageSource.getMessage("contact.topic.account", null, "Probleem met account", locale);
            case "feedback" -> messageSource.getMessage("contact.topic.feedback", null, "Feedback of suggestie", locale);
            default -> messageSource.getMessage("contact.topic.other", null, "Overig", locale);
        };
    }

    private String buildPlainText(String userEmail, String readableTopic, String subject, String message) {
        return "Nieuw contactbericht via CloudGuard\n\n" +
                "Afzender: " + userEmail + "\n" +
                "Categorie: " + readableTopic + "\n" +
                "Onderwerp: " + subject + "\n\n" +
                "Bericht:\n" + message;
    }

    private String buildHtmlContent(String userEmail, String readableTopic, String subject, String message, Locale locale) {
        String extraCss =
                """
                        .title { font-size: 20px; font-weight: 600; color: %s; margin: 0 0 24px 0; }
                        .card { background: %s; border-radius: 8px; padding: 20px; margin-bottom: 16px; border: 1px solid #e5e7eb; }
                        .label { font-size: 12px; font-weight: 600; color: %s; text-transform: uppercase; letter-spacing: 0.5px; margin-bottom: 4px; }
                        .value { font-size: 15px; color: %s; margin-bottom: 16px; font-weight: 500; }
                        .user-email { color: #0284c7; text-decoration: none; font-weight: 600; }
                        .message-box { background: #f8fafc; border-left: 4px solid %s; padding: 16px; border-radius: 4px; margin-top: 8px; white-space: pre-wrap; color: #334155; font-size: 14px; line-height: 1.6; }
                        """
                        .formatted(
                                SecurityEmailHtml.FOREGROUND,
                                SecurityEmailHtml.CARD_BG,
                                SecurityEmailHtml.MUTED_TEXT,
                                SecurityEmailHtml.FOREGROUND,
                                SecurityEmailHtml.PRIMARY);

        String link = appPublicUrl != null ? appPublicUrl.trim() : "";
        String buttonLabel = messageSource.getMessage("email.contact.button", null, "Naar CloudGuard", locale);
        String buttonBlock = link.isEmpty() ? "" : """
                <div style="margin-top: 24px; text-align: center;">
                    <a href="%s" style="display: inline-block; background: %s; color: #ffffff; padding: 12px 24px; border-radius: 6px; text-decoration: none; font-weight: 600;">%s</a>
                </div>
                """.formatted(safeHref(link), SecurityEmailHtml.PRIMARY, UtilityFunctions.escapeHtml(buttonLabel));

        String content =
                """
                          <h2 class="title">Nieuw contactbericht via CloudGuard</h2>
                          <div class="card">
                            <div class="label">AFZENDER</div>
                            <div class="value"><a href="mailto:%s" class="user-email">%s</a></div>
                            
                            <div class="label">CATEGORIE</div>
                            <div class="value">%s</div>
                            
                            <div class="label">ONDERWERP</div>
                            <div class="value">%s</div>
                            
                            <div class="label">BERICHT</div>
                            <div class="message-box">%s</div>
                          </div>
                          %s
                        """
                        .formatted(
                                UtilityFunctions.escapeHtml(userEmail),
                                UtilityFunctions.escapeHtml(userEmail),
                                UtilityFunctions.escapeHtml(readableTopic),
                                UtilityFunctions.escapeHtml(subject),
                                UtilityFunctions.escapeHtml(message),
                                buttonBlock);

        String footer =
                UtilityFunctions.escapeHtml(
                        "U kunt direct op deze e-mail reageren om de gebruiker te antwoorden.");

        return SecurityEmailHtml.document(extraCss, true, content, footer);
    }

    private static String safeHref(String url) {
        if (url == null) {
            return "";
        }
        return UtilityFunctions.escapeHtml(url).replace(" ", "%20");
    }
}
