package com.cloudmen.cloudguard.integration.service.report;

import com.cloudmen.cloudguard.exception.FailedFeedbackEmailException;
import com.cloudmen.cloudguard.service.AdminSecurityKeysService;
import com.cloudmen.cloudguard.service.report.SecurityReportEmailService;
import com.cloudmen.cloudguard.utility.GoogleApiFactory;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.MessageSource;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = {SecurityReportEmailService.class})
public class SecurityReportEmailServiceIT {

    @Autowired
    SecurityReportEmailService securityReportEmailService;

    @MockitoBean
    private JavaMailSender mailSender;

    @MockitoBean(name = "messageSource")
    private MessageSource messageSource;

    @MockitoBean
    private GoogleApiFactory googleApiFactory;

    @MockitoBean
    private AdminSecurityKeysService adminSecurityKeysService;

    @BeforeEach
    void setUp() {
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage( (Session) null));

        when(messageSource.getMessage(any(String.class), any(), any(Locale.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void sendSecurityReportEmail_success_sendsWithAttachment() {
        byte[] dummyData = new byte[]{1, 2, 3, 4};

        securityReportEmailService.sendSecurityReportEmail(
                "admin@cloudmen.com",
                "Cloudmen BV",
                dummyData,
                Locale.ENGLISH
        );

        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void sendSecurityReportEmail_success_sendsWithoutAttachment() {
        securityReportEmailService.sendSecurityReportEmail(
                "admin@cloudmen.com",
                "Cloudmen BV",
                null,
                Locale.ENGLISH
        );

        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void sendSecurityReportEmail_mailException_throwsCustomException() {
        doThrow(new MailSendException("SMTP error"))
                .when(mailSender).send(any(MimeMessage.class));

        assertThrows(FailedFeedbackEmailException.class, () -> securityReportEmailService.sendSecurityReportEmail(
                "admin@cloudmen.com",
                "Cloudmen BV",
                new byte[]{1, 2, 3},
                Locale.ENGLISH
        ));
    }
}
