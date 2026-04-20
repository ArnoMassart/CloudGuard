package com.cloudmen.cloudguard.service.notification;

import com.cloudmen.cloudguard.exception.FailedFeedbackEmailException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import com.cloudmen.cloudguard.utility.UtilityFunctions;

import java.util.List;
import java.util.Locale;

@Service
public class FeedbackEmailService {

    private static final String HEADER_BG = "#011624";
    private static final String PRIMARY = "#3abfad";
    private static final String MUTED_BG = "#ececf0";
    private static final String MUTED_TEXT = "#717182";
    private static final String CARD_BG = "#ffffff";
    private static final String FOREGROUND = "#030213";

    private final JavaMailSender mailSender;
    private final MessageSource messageSource;

    @Value("${app.feedback.notification-emails:}")
    private List<String> recipientEmails;

    @Value("${spring.mail.username:noreply@cloudguard.com}")
    private String fromEmail;

    public FeedbackEmailService(JavaMailSender mailSender, @Qualifier("messageSource") MessageSource messageSource) {
        this.mailSender = mailSender;
        this.messageSource = messageSource;
    }

    public void sendFeedbackEmail(String userId, String source, String notificationType, String feedbackText) {
        if (recipientEmails == null || recipientEmails.isEmpty()) return;

        Locale locale = LocaleContextHolder.getLocale();
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(recipientEmails.toArray(new String[0]));
            helper.setSubject(messageSource.getMessage(
                    "email.feedback.subject", new Object[]{notificationType}, locale));
            try {
                helper.setFrom(fromEmail, "CloudGuard");
            } catch (Exception e) {
                throw new MessagingException(e.getMessage());
            }
            helper.setText(
                    buildPlainText(locale, userId, source, notificationType, feedbackText),
                    buildHtmlContent(locale, userId, source, notificationType, feedbackText));
            mailSender.send(message);
        } catch (MessagingException e) {
            throwFeedbackEmailFailed(e);
        } catch (MailException e) {
            throwFeedbackEmailFailed(e);
        }
    }

    private void throwFeedbackEmailFailed(Throwable cause) {
        String msg = messageSource.getMessage("api.email.send_failed", null, LocaleContextHolder.getLocale());
        throw new FailedFeedbackEmailException(msg, cause);
    }

    private String buildPlainText(Locale locale, String userId, String source, String notificationType, String feedbackText) {
        String intro = messageSource.getMessage("email.feedback.intro", null, locale);
        String userLabel = messageSource.getMessage("email.feedback.label.user", null, locale);
        String sourceLabel = messageSource.getMessage("email.feedback.label.source", null, locale);
        String typeLabel = messageSource.getMessage("email.feedback.label.type", null, locale);
        String fbLabel = messageSource.getMessage("email.feedback.label.feedback", null, locale);

        return intro + "\n\n" +
                userLabel + ": " + userId + "\n" +
                sourceLabel + ": " + source + "\n" +
                typeLabel + ": " + notificationType + "\n\n" +
                fbLabel + ":\n" + feedbackText;
    }

    private String buildHtmlContent(Locale locale, String userId, String source, String notificationType, String feedbackText) {
        String escapedFeedback = UtilityFunctions.escapeHtml(feedbackText);
        String titleText = messageSource.getMessage("email.feedback.html.title", null, locale);
        String userLabel = messageSource.getMessage("email.feedback.label.user", null, locale);
        String sourceLabel = messageSource.getMessage("email.feedback.label.source", null, locale);
        String typeLabel = messageSource.getMessage("email.feedback.label.type", null, locale);
        String fbLabel = messageSource.getMessage("email.feedback.label.feedback", null, locale);
        String footerText = messageSource.getMessage("email.feedback.footer", null, locale);

        return """
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
              <style>
                body { margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, sans-serif; background-color: #f3f3f5; }
                .container { max-width: 600px; margin: 20px auto; background: %s; border-radius: 8px; overflow: hidden; border: 1px solid #e5e7eb; }
                .header { background: %s; padding: 24px 32px; text-align: center; color: white; }
                .content { padding: 32px; color: %s; line-height: 1.5; }
                .title { margin: 0 0 24px 0; color: %s; }
                .card { background: %s; border-radius: 8px; padding: 20px; margin-bottom: 16px; border: 1px solid #e5e7eb; }
                .label { font-size: 12px; font-weight: 600; color: %s; text-transform: uppercase; letter-spacing: 0.5px; margin-bottom: 4px; }
                .value { font-size: 15px; color: %s; margin-bottom: 16px; }
                .feedback-box { background: %s; border-left: 4px solid %s; padding: 16px; border-radius: 4px; margin-top: 8px; white-space: pre-wrap; }
                .footer { background: %s; padding: 20px 32px; font-size: 12px; color: %s; text-align: center; }
              </style>
            </head>
            <body>
              <div class="container">
                <div class="header">
                  <h2 style="margin: 0;">CloudGuard Security</h2>
                </div>
                <div class="content">
                  <h3 class="title">%s</h3>
                  <div class="card">
                    <div class="label">%s</div>
                    <div class="value">%s</div>
                    <div class="label">%s</div>
                    <div class="value">%s</div>
                    <div class="label">%s</div>
                    <div class="value">%s</div>
                    <div class="label">%s</div>
                    <div class="feedback-box">%s</div>
                  </div>
                </div>
                <div class="footer">
                  %s
                </div>
              </div>
            </body>
            </html>
            """.formatted(
                CARD_BG,
                HEADER_BG,
                FOREGROUND,
                FOREGROUND,
                CARD_BG,
                MUTED_TEXT,
                FOREGROUND,
                MUTED_BG,
                PRIMARY,
                MUTED_BG,
                MUTED_TEXT,
                UtilityFunctions.escapeHtml(titleText),
                UtilityFunctions.escapeHtml(userLabel),
                UtilityFunctions.escapeHtml(userId),
                UtilityFunctions.escapeHtml(sourceLabel),
                UtilityFunctions.escapeHtml(source),
                UtilityFunctions.escapeHtml(typeLabel),
                UtilityFunctions.escapeHtml(notificationType),
                UtilityFunctions.escapeHtml(fbLabel),
                escapedFeedback,
                UtilityFunctions.escapeHtml(footerText)
        );
    }
}
