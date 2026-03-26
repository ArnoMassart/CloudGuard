package com.cloudmen.cloudguard.service.report;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class SecurityReportEmailService {
    private static final String HEADER_BG = "#011624";
    private static final String PRIMARY = "#3abfad";
    private static final String MUTED_BG = "#ececf0";
    private static final String MUTED_TEXT = "#717182";
    private static final String CARD_BG = "#ffffff";
    private static final String FOREGROUND = "#030213";

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply@cloudguard.com}")
    private String fromEmail;

    public SecurityReportEmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Async
    public void sendSecurityReportEmail(String recipientEmail, String companyName, byte[] pdfBytes) {
        String subject = "CloudGuard: Uw halfjaarlijkse Security Rapport";
        String plainText = "Beste beheerder,\n\nIn de bijlage vindt u het halfjaarlijkse CloudGuard Security Rapport voor uw organisatie.";

        String htmlBody = buildReportHtml(companyName);
        String fileName = "CloudGuard_Security_Rapport_" + companyName.replace(" ", "_") + ".pdf";

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

    private String buildReportHtml(String companyName) {
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
                      <h3 style="margin-top: 0; color: %s;">Beste beheerder van %s,</h3>
                      <p>In de bijlage van deze e-mail vindt u het geautomatiseerde beveiligingsrapport van uw IT-omgeving.</p>
                      <p>Dit document bevat een gedetailleerde analyse van uw inlogbeveiliging (MFA), externe applicaties, en domeinconfiguraties (SPF/DKIM/DMARC).</p>
                      <br>
                      <p>Met vriendelijke groet,<br><strong>Het CloudGuard Team</strong></p>
                    </div>
                    <div class="footer">
                      Deze e-mail is automatisch gegenereerd.
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(CARD_BG, HEADER_BG, FOREGROUND, MUTED_BG, MUTED_TEXT, HEADER_BG, escapeHtml(companyName));
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
