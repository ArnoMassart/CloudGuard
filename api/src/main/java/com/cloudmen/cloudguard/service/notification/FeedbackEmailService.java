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

import java.util.List;

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

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(recipientEmails.toArray(new String[0]));
            helper.setSubject("CloudGuard: Nieuwe feedback – " + notificationType);
            try{
                helper.setFrom(fromEmail, "CloudGuard");
            }catch(Exception e){
                throw new MessagingException(e.getMessage());
            }
            helper.setText(buildPlainText(userId, source, notificationType, feedbackText), buildHtmlContent(userId, source, notificationType, feedbackText));
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

    private String buildPlainText(String userId, String source, String notificationType, String feedbackText) {
        return String.format(
                "Er is nieuwe feedback ontvangen.\n\n" +
                        "Gebruiker: %s\n" +
                        "Bron: %s\n" +
                        "Meldingstype: %s\n\n" +
                        "Feedback:\n%s",
                userId, source, notificationType, feedbackText
        );
    }

    private String buildHtmlContent(String userId, String source, String notificationType, String feedbackText) {
        String escapedFeedback = escapeHtml(feedbackText);

        return """
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="UTF-8">
              <meta name="viewport" content="width=device-width, initial-scale=1.0">
              <style>
                body { margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background-color: #f3f3f5; }
                .container { max-width: 600px; margin: 0 auto; background: %s; }
                .header { background: %s; padding: 24px 32px; text-align: left; }
                .logo { display: flex; align-items: center; gap: 12px; }
                .logo-icon { font-size: 28px; line-height: 1; }
                .logo-text { color: #ffffff; font-size: 24px; font-weight: 700; letter-spacing: -0.5px; margin: 0; }
                .content { padding: 32px; color: %s; }
                .title { font-size: 20px; font-weight: 600; color: %s; margin: 0 0 24px 0; }
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
                  <div class="logo">
                    <span class="logo-icon">&#128737;</span>
                    <h1 class="logo-text">CloudGuard</h1>
                  </div>
                </div>
                <div class="content">
                  <h2 class="title">Nieuwe feedback ontvangen</h2>
                  <div class="card">
                    <div class="label">Gebruiker</div>
                    <div class="value">%s</div>
                    <div class="label">Bron</div>
                    <div class="value">%s</div>
                    <div class="label">Meldingstype</div>
                    <div class="value">%s</div>
                    <div class="label">Feedback</div>
                    <div class="feedback-box">%s</div>
                  </div>
                </div>
                <div class="footer">
                  Deze e-mail is automatisch verzonden door CloudGuard Security Dashboard.
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
                escapeHtml(userId),
                escapeHtml(source),
                escapeHtml(notificationType),
                escapedFeedback
        );
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
