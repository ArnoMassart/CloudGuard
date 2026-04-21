package com.cloudmen.cloudguard.service.reminder;

import com.cloudmen.cloudguard.domain.model.notification.NotificationInstance;
import com.cloudmen.cloudguard.utility.UtilityFunctions;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.cloudmen.cloudguard.utility.SecurityEmailHtml;

import java.util.List;
import java.util.Locale;

/**
 * Sends the weekly critical-notification digest. Mail failures return {@code false} so the batch job can continue.
 */
@Service
public class CriticalNotificationReminderEmailService {

    private final JavaMailSender mailSender;
    private final MessageSource messageSource;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    @Value("${cloudguard.app.public-url:}")
    private String appPublicUrl;

    public CriticalNotificationReminderEmailService(
            JavaMailSender mailSender, @Qualifier("messageSource") MessageSource messageSource) {
        this.mailSender = mailSender;
        this.messageSource = messageSource;
    }

    public boolean sendWeeklyCriticalDigest(String toEmail, Locale locale, List<NotificationInstance> criticalItems) {
        if (fromEmail == null || fromEmail.isBlank()) {
            return false;
        }
        if (toEmail == null || toEmail.isBlank()) {
            return false;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(toEmail);
            helper.setSubject(messageSource.getMessage("email.reminder.critical.subject", null, locale));
            try {
                helper.setFrom(fromEmail, "CloudGuard");
            } catch (Exception e) {
                throw new MessagingException(e.getMessage(), e);
            }
            helper.setText(buildPlainText(locale, criticalItems), buildHtml(locale, criticalItems));
            mailSender.send(message);
            return true;
        } catch (MessagingException | MailException e) {
            return false;
        }
    }

    private String buildPlainText(Locale locale, List<NotificationInstance> items) {
        StringBuilder sb = new StringBuilder();
        sb.append(messageSource.getMessage("email.reminder.critical.intro", null, locale)).append("\n\n");
        for (NotificationInstance n : items) {
            String line =
                    messageSource.getMessage(
                            "email.reminder.critical.item",
                            new Object[] {itemTitle(n, locale), itemSubtitle(n)},
                            locale);
            sb.append("• ").append(line).append("\n");
        }
        sb.append("\n\n")
                .append(messageSource.getMessage("email.reminder.critical.footer", null, locale));
        String link = appPublicUrl != null ? appPublicUrl.trim() : "";
        if (!link.isEmpty()) {
            sb.append("\n").append(link);
        }
        return sb.toString();
    }

    private String buildHtml(Locale locale, List<NotificationInstance> items) {
        String intro = UtilityFunctions.escapeHtml(messageSource.getMessage("email.reminder.critical.intro", null, locale));
        String cta = UtilityFunctions.escapeHtml(messageSource.getMessage("email.reminder.critical.footer", null, locale));
        String buttonLabel = UtilityFunctions.escapeHtml(messageSource.getMessage("email.reminder.critical.button", null, locale));
        String footer = UtilityFunctions.escapeHtml(messageSource.getMessage("email.report.footer", null, locale));

        StringBuilder itemBlocks = new StringBuilder();
        for (NotificationInstance n : items) {
            String title = UtilityFunctions.escapeHtml(itemTitle(n, locale));
            String sub = UtilityFunctions.escapeHtml(itemSubtitle(n));
            itemBlocks.append(
                    """
                    <p style="margin: 16px 0 0 0; padding: 12px 16px; background: %s; border-radius: 6px; border-left: 4px solid %s;">
                    <strong style="color: %s;">%s</strong><br>
                    <span style="font-size: 14px; color: %s;">%s</span>
                    </p>
                    """
                            .formatted(
                                    SecurityEmailHtml.MUTED_BG,
                                    SecurityEmailHtml.HEADER_BG,
                                    SecurityEmailHtml.FOREGROUND,
                                    title,
                                    SecurityEmailHtml.MUTED_TEXT,
                                    sub));
        }

        String link = appPublicUrl != null ? appPublicUrl.trim() : "";
        String buttonBlock =
                link.isEmpty()
                        ? ""
                        : """
 <p style="margin: 16px 0 0 0; text-align: center;">
 <a href="%s" style="display: inline-block; background: %s; color: #ffffff; padding: 12px 24px; border-radius: 6px; text-decoration: none; font-weight: 600;">%s</a>
                            </p>
                            """
                                .formatted(safeHref(link), SecurityEmailHtml.HEADER_BG, buttonLabel);

        String content =
                """
                      <h3 style="margin-top: 0; color: %s;">%s</h3>
                      %s
                      <p style="margin: 24px 0 0 0;">%s</p>
                      %s
                      """
                        .formatted(
                                SecurityEmailHtml.HEADER_BG,
                                intro,
                                itemBlocks,
                                cta,
                                buttonBlock);

        return SecurityEmailHtml.document(content, footer);
    }

    private static String safeHref(String url) {
        if (url == null) {
            return "";
        }
        return UtilityFunctions.escapeHtml(url).replace(" ", "%20");
    }

    private String itemTitle(NotificationInstance n, Locale locale) {
        if (n.getTitle() != null && !n.getTitle().isBlank()) {
            return n.getTitle();
        }
        return messageSource.getMessage("email.reminder.critical.untitled", null, locale);
    }

    private String itemSubtitle(NotificationInstance n) {
        if (n.getSourceLabel() != null && !n.getSourceLabel().isBlank()) {
            return n.getSourceLabel();
        }
        return n.getSource() != null ? n.getSource() : "";
    }
}
