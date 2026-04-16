package com.cloudmen.cloudguard.unit.service.notification;

import com.cloudmen.cloudguard.domain.model.notification.NotificationInstance;
import com.cloudmen.cloudguard.service.reminder.CriticalNotificationReminderEmailService;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CriticalNotificationReminderEmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private MessageSource messageSource;

    private CriticalNotificationReminderEmailService service;

    @BeforeEach
    void setUp() {
        service = new CriticalNotificationReminderEmailService(mailSender, messageSource);
        ReflectionTestUtils.setField(service, "fromEmail", "noreply@test.com");
        ReflectionTestUtils.setField(service, "appPublicUrl", "https://app.example.com");
        lenient()
                .when(messageSource.getMessage(eq("email.reminder.critical.subject"), isNull(), eq(Locale.ENGLISH)))
                .thenReturn("Subject");
        lenient()
                .when(messageSource.getMessage(eq("email.reminder.critical.intro"), isNull(), eq(Locale.ENGLISH)))
                .thenReturn("Intro");
        lenient()
                .when(messageSource.getMessage(eq("email.reminder.critical.footer"), isNull(), eq(Locale.ENGLISH)))
                .thenReturn("Footer");
        lenient()
                .when(
                        messageSource.getMessage(
                                eq("email.reminder.critical.item"), any(Object[].class), eq(Locale.ENGLISH)))
                .thenReturn("Item line");
        lenient()
                .when(messageSource.getMessage(eq("email.reminder.critical.untitled"), isNull(), eq(Locale.ENGLISH)))
                .thenReturn("Untitled");
        lenient()
                .when(messageSource.getMessage(eq("email.reminder.critical.button"), isNull(), eq(Locale.ENGLISH)))
                .thenReturn("Open CloudGuard");
        lenient()
                .when(messageSource.getMessage(eq("email.report.footer"), isNull(), eq(Locale.ENGLISH)))
                .thenReturn("This email was generated automatically.");
    }

    @Test
    void sendWeeklyCriticalDigest_returnsFalseWhenFromAddressBlank() {
        ReflectionTestUtils.setField(service, "fromEmail", " ");
        assertFalse(service.sendWeeklyCriticalDigest("user@test.com", Locale.ENGLISH, List.of(new NotificationInstance())));
    }

    @Test
    void sendWeeklyCriticalDigest_returnsFalseWhenToAddressBlank() {
        assertFalse(service.sendWeeklyCriticalDigest("", Locale.ENGLISH, List.of(new NotificationInstance())));
    }

    @Test
    void sendWeeklyCriticalDigest_returnsTrueAndSendsWhenMailOk() throws Exception {
        MimeMessage mimeMessage = new MimeMessage(jakarta.mail.Session.getInstance(new java.util.Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        NotificationInstance n = new NotificationInstance();
        n.setTitle("Critical finding");
        n.setSourceLabel("DNS");
        assertTrue(service.sendWeeklyCriticalDigest("admin@test.com", Locale.ENGLISH, List.of(n)));

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
    }

    @Test
    void sendWeeklyCriticalDigest_returnsFalseWhenSendFails() throws Exception {
        MimeMessage mimeMessage = new MimeMessage(jakarta.mail.Session.getInstance(new java.util.Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new MailSendException("fail")).when(mailSender).send(any(MimeMessage.class));

        assertFalse(
                service.sendWeeklyCriticalDigest("admin@test.com", Locale.ENGLISH, List.of(new NotificationInstance())));
    }
}
