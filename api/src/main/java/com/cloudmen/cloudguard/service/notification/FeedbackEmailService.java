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

import com.cloudmen.cloudguard.utility.SecurityEmailHtml;
import com.cloudmen.cloudguard.utility.UtilityFunctions;

import java.util.List;
import java.util.Locale;

@Service
public class FeedbackEmailService {

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

        String extraCss =
                """
                        .title { margin: 0 0 24px 0; color: %s; }
                        .card { background: %s; border-radius: 8px; padding: 20px; margin-bottom: 16px; border: 1px solid #e5e7eb; }
                        .label { font-size: 12px; font-weight: 600; color: %s; text-transform: uppercase; letter-spacing: 0.5px; margin-bottom: 4px; }
                        .value { font-size: 15px; color: %s; margin-bottom: 16px; }
                        .feedback-box { background: %s; border-left: 4px solid %s; padding: 16px; border-radius: 4px; margin-top: 8px; white-space: pre-wrap; }
                        """
                        .formatted(
                                SecurityEmailHtml.FOREGROUND,
                                SecurityEmailHtml.CARD_BG,
                                SecurityEmailHtml.MUTED_TEXT,
                                SecurityEmailHtml.FOREGROUND,
                                SecurityEmailHtml.MUTED_BG,
                                SecurityEmailHtml.PRIMARY);

        String content =
                """
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
                        """
                        .formatted(
                                UtilityFunctions.escapeHtml(titleText),
                                UtilityFunctions.escapeHtml(userLabel),
                                UtilityFunctions.escapeHtml(userId),
                                UtilityFunctions.escapeHtml(sourceLabel),
                                UtilityFunctions.escapeHtml(source),
                                UtilityFunctions.escapeHtml(typeLabel),
                                UtilityFunctions.escapeHtml(notificationType),
                                UtilityFunctions.escapeHtml(fbLabel),
                                escapedFeedback);

        return SecurityEmailHtml.document(
                extraCss, true, content, UtilityFunctions.escapeHtml(footerText));
    }
}
