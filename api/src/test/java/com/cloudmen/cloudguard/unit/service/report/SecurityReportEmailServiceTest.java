package com.cloudmen.cloudguard.unit.service.report;

import com.cloudmen.cloudguard.service.report.SecurityReportEmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.mail.javamail.JavaMailSender;
import com.cloudmen.cloudguard.exception.FailedFeedbackEmailException;
import jakarta.mail.BodyPart;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SecurityReportEmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private MessageSource messageSource;

    private SecurityReportEmailService service;

    @BeforeEach
    void setUp(){
        service = new SecurityReportEmailService(mailSender, messageSource);
        ReflectionTestUtils.setField(service, "fromEmail", "noreply@cloudguard.com");

        lenient().when(messageSource.getMessage(anyString(), any(), any())).thenAnswer(invocation -> {
            String code = invocation.getArgument(0);
            return "Translated_" + code;
        });
    }

    @Test
    void sendSecurityReportEmail_successWithAttachment() throws Exception {
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        byte[] dummyPdf = "Dummy PDF Content".getBytes(StandardCharsets.UTF_8);

        service.sendSecurityReportEmail("admin@example.com", "Cloudmen BV", dummyPdf, Locale.ENGLISH);
        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());

        MimeMessage sentMessage = captor.getValue();
        assertEquals("Translated_email.report.subject", sentMessage.getSubject());
        assertEquals("admin@example.com", sentMessage.getAllRecipients()[0].toString());

        Multipart multipart = (Multipart) sentMessage.getContent();
        boolean foundAttachment = false;
        for (int i = 0; i < multipart.getCount(); i++) {
            BodyPart bodyPart = multipart.getBodyPart(i);
            if (Part.ATTACHMENT.equalsIgnoreCase(bodyPart.getDisposition())) {
                foundAttachment = true;
                assertEquals("Translated_email.report.filename_Cloudmen_BV.pdf", bodyPart.getFileName());
            }
        }
        assertTrue(foundAttachment, "De PDF attachment ontbreekt in de email");
    }

    @Test
    void sendSecurityReportEmail_successWithoutAttachment() throws Exception {
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        service.sendSecurityReportEmail("admin@example.com", "Cloudmen BV", null, Locale.ENGLISH);

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());

        Object content = captor.getValue().getContent();
        if (content instanceof Multipart multipart) {
            for (int i = 0; i < multipart.getCount(); i++) {
                assertNotEquals(Part.ATTACHMENT, multipart.getBodyPart(i).getDisposition());
            }
        }
    }

    @Test
    void sendSecurityReportEmail_escapesHtmlInCompanyName() throws Exception {
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        when(messageSource.getMessage(eq("email.report.greeting"), any(), any())).thenAnswer(invocation -> {
            Object[] args = invocation.getArgument(1);
            return "Beste " + args[0];
        });

        String maliciousCompanyName = "<script>alert('hack')</script> & \"Corp\"";

        service.sendSecurityReportEmail("admin@example.com", maliciousCompanyName, null, Locale.ENGLISH);

        verify(mailSender).send(mimeMessage);
        String combinedHtml = flattenContent(mimeMessage.getContent());

        assertTrue(combinedHtml.contains("Beste &lt;script&gt;alert(&#39;hack&#39;)&lt;/script&gt; &amp; &quot;Corp&quot;"));
        assertFalse(combinedHtml.contains("<script>"));
    }

    @Test
    void sendSecurityReportEmail_throwsFailedFeedbackEmailException_onMessagingError() {
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        ReflectionTestUtils.setField(service, "fromEmail", null);

        FailedFeedbackEmailException exception = assertThrows(FailedFeedbackEmailException.class, () -> {
            service.sendSecurityReportEmail("admin@example.com", "Test", null, Locale.ENGLISH);
        });

        assertEquals(messageSource.getMessage("api.email.send_failed", null, Locale.ENGLISH), exception.getMessage());
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    // ==========================================
    // Helpers om Multipart Content uit te lezen
    // ==========================================

    private static String flattenContent(Object content) throws Exception {
        if (content == null) {
            return "";
        }
        if (content instanceof String s) {
            return s;
        }
        if (content instanceof Multipart mp) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < mp.getCount(); i++) {
                BodyPart part = mp.getBodyPart(i);
                sb.append(flattenPart(part));
            }
            return sb.toString();
        }
        if (content instanceof InputStream in) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        return "";
    }

    private static String flattenPart(Part part) throws Exception {
        Object c = part.getContent();
        if (c instanceof String s) {
            return s;
        }
        if (c instanceof Multipart mp) {
            return flattenContent(mp);
        }
        try (InputStream in = part.getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
