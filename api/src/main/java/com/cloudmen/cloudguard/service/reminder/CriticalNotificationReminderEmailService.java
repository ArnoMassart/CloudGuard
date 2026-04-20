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

import java.util.List;
import java.util.Locale;

/**
 * Sends the weekly critical-notification digest. Mail failures return {@code false} so the batch job can continue.
 */
@Service
public class CriticalNotificationReminderEmailService {

    private static final String HEADER_BG = "#011624";
    private static final String MUTED_BG = "#ececf0";
    private static final String MUTED_TEXT = "#717182";
    private static final String CARD_BG = "#ffffff";
    private static final String FOREGROUND = "#030213";

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
                            .formatted(MUTED_BG, HEADER_BG, FOREGROUND, title, MUTED_TEXT, sub));
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
                                .formatted(safeHref(link), HEADER_BG, buttonLabel);

        return """
                <!DOCTYPE html>
                <html>
                <head>
                  <meta charset="UTF-8">
                  <style>
                    body { margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, sans-serif; background-color: #f3f3f5; }
                    .container { max-width: 600px; margin: 20px auto; background: %s; border-radius: 8px; overflow: hidden; border: 1px solid #e5e7eb; }
                    .header { background: %s; padding: 24px 32px; text-align: center; color: white; }
                    .content { padding: 32px; color: %s; line-height: 1.5; }
                    .footer { background: %s; padding: 20px 32px; font-size: 12px; color: %s; text-align: center; }
                  </style>
                </head>
                <body>
                  <div class="container">
                    <div class="header">
                      <h2 style="margin: 0;">CloudGuard Security</h2>
                    </div>
                    <div class="content">
                      <h3 style="margin-top: 0; color: %s;">%s</h3>
                      %s
                      <p style="margin: 24px 0 0 0;">%s</p>
                      %s
                    </div>
                    <div class="footer">
                      %s
                    </div>
                  </div>
                </body>
                </html>
                """
                .formatted(
                        CARD_BG,
                        HEADER_BG,
                        FOREGROUND,
                        MUTED_BG,
                        MUTED_TEXT,
                        HEADER_BG,
                        intro,
                        itemBlocks,
                        cta,
                        buttonBlock,
                        footer);
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
