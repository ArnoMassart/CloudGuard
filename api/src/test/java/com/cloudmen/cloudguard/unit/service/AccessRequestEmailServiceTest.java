package com.cloudmen.cloudguard.unit.service;

import com.cloudmen.cloudguard.service.AccessRequestEmailService;
import com.cloudmen.cloudguard.utility.SecurityEmailHtml;
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
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccessRequestEmailServiceTest {

    @Mock
    JavaMailSender mailSender;

    @Mock
    MessageSource messageSource;

    AccessRequestEmailService service;

    @BeforeEach
    void setUp() {
        service = new AccessRequestEmailService(mailSender, messageSource);
        ReflectionTestUtils.setField(service, "recipientEmails", List.of("ops@example.com"));
        ReflectionTestUtils.setField(service, "fromEmail", "noreply@example.com");

        lenient().when(messageSource.getMessage(anyString(), isNull(), anyString(), any())).thenAnswer(inv -> inv.getArgument(2));
        lenient().when(messageSource.getMessage(anyString(), isNull(), any())).thenAnswer(inv -> inv.getArgument(2));
    }

    @Test
    void notifyOrganizationRequest_htmlUsesSharedEmailShell() throws Exception {
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        service.notifyOrganizationRequest("user@tenant.com");

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        String combined = flattenContent(captor.getValue().getContent());
        assertTrue(combined.contains(SecurityEmailHtml.DEFAULT_HEADER_TITLE));
        assertTrue(combined.contains("margin: 20px auto"));
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
