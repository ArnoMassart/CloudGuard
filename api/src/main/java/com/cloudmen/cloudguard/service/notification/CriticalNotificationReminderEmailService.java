package com.cloudmen.cloudguard.service.notification;

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
        sb.append("\n").append(messageSource.getMessage("email.reminder.critical.footer", null, locale));
        return sb.toString();
    }

    private String buildHtml(Locale locale, List<NotificationInstance> items) {
        StringBuilder rows = new StringBuilder();
        for (NotificationInstance n : items) {
            String title = UtilityFunctions.escapeHtml(itemTitle(n, locale));
            String sub = UtilityFunctions.escapeHtml(itemSubtitle(n));
            rows.append(
                    """
                    <tr><td style="padding:10px 12px;border-bottom:1px solid %s;">
                    <div style="font-weight:600;color:%s;">%s</div>
                    <div style="font-size:13px;color:%s;margin-top:4px;">%s</div>
                    </td></tr>
                    """
                            .formatted(MUTED_BG, FOREGROUND, title, MUTED_TEXT, sub));
        }
        return """
                <!DOCTYPE html>
                <html>
                <body style="margin:0;background:%s;font-family:system-ui,Segoe UI,sans-serif;">
                <div style="max-width:560px;margin:0 auto;padding:24px;">
                  <div style="background:%s;color:#fff;padding:16px 20px;border-radius:12px 12px 0 0;">
                    <div style="font-size:18px;font-weight:600;">CloudGuard</div>
                  </div>
                  <div style="background:%s;color:%s;padding:20px;border-radius:0 0 12px 12px;border:1px solid %s;border-top:none;">
                    <p style="margin:0 0 16px;line-height:1.5;">%s</p>
                    <table style="width:100%%;border-collapse:collapse;">%s</table>
                    <p style="margin:16px 0 0;font-size:14px;color:%s;">%s</p>
                  </div>
                </div>
                </body>
                </html>
                """
                .formatted(
                        MUTED_BG,
                        HEADER_BG,
                        CARD_BG,
                        FOREGROUND,
                        MUTED_BG,
                        UtilityFunctions.escapeHtml(messageSource.getMessage("email.reminder.critical.intro", null, locale)),
                        rows,
                        MUTED_TEXT,
                        UtilityFunctions.escapeHtml(messageSource.getMessage("email.reminder.critical.footer", null, locale)));
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
