package com.cloudmen.cloudguard.unit.service.notification;

import com.cloudmen.cloudguard.service.notification.FeedbackEmailService;
import com.cloudmen.cloudguard.utility.SecurityEmailHtml;
import com.cloudmen.cloudguard.exception.FailedFeedbackEmailException;
import jakarta.mail.BodyPart;
import jakarta.mail.Part;
import jakarta.mail.Multipart;
import jakarta.mail.Session;
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

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FeedbackEmailServiceTest {

    @Mock
    JavaMailSender mailSender;
    @Mock
    MessageSource messageSource;

    FeedbackEmailService service;

    @BeforeEach
    void setUp() {
        service = new FeedbackEmailService(mailSender, messageSource);
        stubMessageSourceDefaults();
    }

    private void stubMessageSourceDefaults() {
        lenient().when(messageSource.getMessage(eq("email.feedback.subject"), any(), any()))
                .thenReturn("CloudGuard: New feedback – test");
        lenient().when(messageSource.getMessage(eq("email.feedback.intro"), isNull(), any()))
                .thenReturn("New feedback has been received.");
        lenient().when(messageSource.getMessage(eq("email.feedback.label.user"), isNull(), any()))
                .thenReturn("User");
        lenient().when(messageSource.getMessage(eq("email.feedback.label.source"), isNull(), any()))
                .thenReturn("Source");
        lenient().when(messageSource.getMessage(eq("email.feedback.label.type"), isNull(), any()))
                .thenReturn("Notification type");
        lenient().when(messageSource.getMessage(eq("email.feedback.label.feedback"), isNull(), any()))
                .thenReturn("Feedback");
        lenient().when(messageSource.getMessage(eq("email.feedback.html.title"), isNull(), any()))
                .thenReturn("New feedback received");
        lenient().when(messageSource.getMessage(eq("email.feedback.footer"), isNull(), any()))
                .thenReturn("This email was sent automatically by CloudGuard Security Dashboard.");
    }

    @Test
    void sendFeedbackEmail_noRecipientsConfigured_doesNotSend() {
        ReflectionTestUtils.setField(service, "recipientEmails", List.of());

        service.sendFeedbackEmail("u1", "s", "t", "hello");

        verify(mailSender, never()).createMimeMessage();
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void sendFeedbackEmail_nullRecipientList_doesNotSend() {
        ReflectionTestUtils.setField(service, "recipientEmails", null);

        service.sendFeedbackEmail("u1", "s", "t", "hello");

        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void sendFeedbackEmail_withRecipients_buildsAndSendsMimeMessage() throws Exception {
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        ReflectionTestUtils.setField(service, "recipientEmails", List.of("ops@example.com"));
        ReflectionTestUtils.setField(service, "fromEmail", "noreply@example.com");

        service.sendFeedbackEmail("user@tenant.com", "users-groups", "user-control", "My feedback");

        verify(mailSender).createMimeMessage();
        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        assertSame(mimeMessage, captor.getValue());
        assertNotNull(mimeMessage.getSubject());
        String combined = flattenContent(mimeMessage.getContent());
        assertTrue(combined.contains("user@tenant.com"));
        assertTrue(combined.contains("users-groups"));
        assertTrue(combined.contains("user-control"));
        assertTrue(combined.contains("My feedback"));
        assertTrue(combined.contains(SecurityEmailHtml.DEFAULT_HEADER_TITLE));
        assertTrue(combined.contains("margin: 20px auto"));
    }

    @Test
    void sendFeedbackEmail_sendFails_throwsFailedFeedbackEmailException() throws Exception {
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new MailSendException("smtp error")).when(mailSender).send(any(MimeMessage.class));

        ReflectionTestUtils.setField(service, "recipientEmails", List.of("ops@example.com"));
        ReflectionTestUtils.setField(service, "fromEmail", "noreply@example.com");
        when(messageSource.getMessage(eq("api.email.send_failed"), isNull(), any())).thenReturn("Could not send");

        FailedFeedbackEmailException ex =
                assertThrows(
                        FailedFeedbackEmailException.class,
                        () -> service.sendFeedbackEmail("u", "s", "t", "hello"));

        assertEquals("Could not send", ex.getMessage());
    }

    @Test
    void sendFeedbackEmail_escapesHtmlInMultipartAlternative() throws Exception {
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        ReflectionTestUtils.setField(service, "recipientEmails", List.of("ops@example.com"));
        ReflectionTestUtils.setField(service, "fromEmail", "noreply@example.com");

        service.sendFeedbackEmail("u", "s", "t", "<script>x</script>");

        verify(mailSender).send(mimeMessage);
        String combined = flattenContent(mimeMessage.getContent());
        assertTrue(combined.contains("&lt;script&gt;"),
                "HTML part should escape angle brackets (plain-text part may still contain raw angle brackets)");
    }

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
