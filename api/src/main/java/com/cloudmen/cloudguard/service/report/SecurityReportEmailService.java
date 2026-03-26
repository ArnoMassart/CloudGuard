package com.cloudmen.cloudguard.service.report;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class SecurityReportEmailService {
    private static final String HEADER_BG = "#011624";
    private static final String MUTED_BG = "#ececf0";
    private static final String MUTED_TEXT = "#717182";
    private static final String CARD_BG = "#ffffff";
    private static final String FOREGROUND = "#030213";

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
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send feedback email", e);
        }
    }

    private String buildReportHtml(String companyName, Locale locale) {
            String safeCompanyName = escapeHtml(companyName);
            String greeting = messageSource.getMessage("email.report.greeting", new Object[]{safeCompanyName}, locale);
            String p1 = messageSource.getMessage("email.report.p1", null, locale);
            String p2 = messageSource.getMessage("email.report.p2", null, locale);
            String signoff = messageSource.getMessage("email.report.signoff", null, locale);
            String footer = messageSource.getMessage("email.report.footer", null, locale);

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
                          <p>%s</p>
                          <p>%s</p>
                          <br>
                          <p>%s<br><strong>Het CloudGuard Team</strong></p>
                        </div>
                        <div class="footer">
                          %s
                        </div>
                      </div>
                    </body>
                    </html>
                    """.formatted(
                    CARD_BG, HEADER_BG, FOREGROUND, MUTED_BG, MUTED_TEXT, HEADER_BG, // CSS Kleuren
                    greeting, p1, p2, signoff, footer // Vertaalde teksten
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
