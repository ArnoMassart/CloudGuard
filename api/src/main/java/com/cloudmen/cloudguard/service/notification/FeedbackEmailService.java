package com.cloudmen.cloudguard.service.notification;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FeedbackEmailService {

    private final JavaMailSender mailSender;

    @Value("${app.feedback.notification-emails:}")
    private List<String> recipientEmails;

    public FeedbackEmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Async
    public void sendFeedbackEmail(String userId, String source, String notificationType, String feedbackText) {
        if (recipientEmails==null || recipientEmails.isEmpty()) return;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(recipientEmails.toArray(new String[0]));
        message.setSubject("Cloudguard feedback: "+ notificationType);
        message.setText(String.format(
                "Er is nieuwe feedback ontvangen.\n\n" +
                        "Gebruiker: %s\n" +
                        "Bron: %s\n" +
                        "Meldingstype: %s\n\n" +
                        "Feedback:\n%s",
                userId, source, notificationType, feedbackText
        ));
        message.setFrom("noreply@cloudguard.com");

        mailSender.send(message);
    }

}
