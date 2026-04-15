package com.cloudmen.cloudguard.service.notification;

import org.springframework.context.MessageSource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class NotificationReminderEmailService {
    private static final String HEADER_BG = "#011624";
    private static final String PRIMARY = "#3abfad";
    private static final String MUTED_BG = "#ececf0";
    private static final String MUTED_TEXT = "#717182";
    private static final String CARD_BG = "#ffffff";
    private static final String FOREGROUND = "#030213";

    private final JavaMailSender mailSender;
    private final MessageSource messageSource;

    public NotificationReminderEmailService(JavaMailSender mailSender, MessageSource messageSource) {
        this.mailSender = mailSender;
        this.messageSource = messageSource;
    }

    
}
