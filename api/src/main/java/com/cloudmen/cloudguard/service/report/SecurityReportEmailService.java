package com.cloudmen.cloudguard.service.report;

import com.cloudmen.cloudguard.exception.FailedFeedbackEmailException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.cloudmen.cloudguard.utility.SecurityEmailHtml;

import java.util.Locale;

@Service
public class SecurityReportEmailService {

    private final JavaMailSender mailSender;
    private final MessageSource messageSource;

    @Value("${spring.mail.username:noreply@cloudguard.com}")
    private String fromEmail;

    public SecurityReportEmailService(JavaMailSender mailSender, @Qualifier("messageSource") MessageSource messageSource) {
        this.mailSender = mailSender;
        this.messageSource = messageSource;
    }

    @Async
    public void sendSecurityReportEmail(String recipientEmail, String companyName, byte[] pdfBytes, Locale locale) {
        String subject = messageSource.getMessage("email.report.subject", null, locale);
        String plainText = messageSource.getMessage("email.report.plaintext", null, locale);

        String htmlBody = buildReportHtml(companyName, locale);
        String filePrefix = messageSource.getMessage("email.report.filename", null, locale);
        String fileName = filePrefix + "_" + companyName.replace(" ", "_") + ".pdf";

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(recipientEmail);
            helper.setSubject(subject);
            try {
                helper.setFrom(fromEmail, "CloudGuard");
            } catch (Exception e) {
                throw new MessagingException(e.getMessage());
            }
            helper.setText(plainText, htmlBody);

            if (pdfBytes != null) {
                helper.addAttachment(fileName, new ByteArrayResource(pdfBytes));
            }
            mailSender.send(message);
        } catch (MessagingException | MailException e) {
            throwReportEmailFailed(locale, e);
        }
    }

    private void throwReportEmailFailed(Locale locale, Throwable cause) {
        String msg = messageSource.getMessage("api.email.send_failed", null, locale);
        throw new FailedFeedbackEmailException(msg, cause);
    }

    private String buildReportHtml(String companyName, Locale locale) {
            String safeCompanyName = escapeHtml(companyName);
            String greeting = messageSource.getMessage("email.report.greeting", new Object[]{safeCompanyName}, locale);
            String p1 = messageSource.getMessage("email.report.p1", null, locale);
            String p2 = messageSource.getMessage("email.report.p2", null, locale);
            String signoff = messageSource.getMessage("email.report.signoff", null, locale);
            String footer = messageSource.getMessage("email.report.footer", null, locale);

            String content =
                    """
                          <h3 style="margin-top: 0; color: %s;">%s</h3>
                          <p>%s</p>
                          <p>%s</p>
                          <br>
                          <p>%s<br><strong>Het CloudGuard Team</strong></p>
                          """
                            .formatted(
                                    SecurityEmailHtml.HEADER_BG,
                                    greeting,
                                    p1,
                                    p2,
                                    signoff);

            return SecurityEmailHtml.document(content, footer);
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
