package com.cloudmen.cloudguard.service.notification;

import com.cloudmen.cloudguard.domain.model.feedback.NotificationFeedback;
import com.cloudmen.cloudguard.repository.NotificationFeedbackRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationFeedbackServiceTest {

    @Mock
    NotificationFeedbackRepository repository;
    @Mock
    FeedbackEmailService emailService;

    NotificationFeedbackService service;

    @BeforeEach
    void setUp() {
        service = new NotificationFeedbackService(repository, emailService);
    }

    @Test
    void submitFeedback_newRow_savesAndSendsEmail() {
        when(repository.findByUserIdAndSourceAndNotificationType("u1", "users-groups", "user-control"))
                .thenReturn(Optional.empty());
        NotificationFeedback saved = new NotificationFeedback();
        saved.setId(1L);
        when(repository.save(any(NotificationFeedback.class))).thenReturn(saved);

        NotificationFeedback out = service.submitFeedback("u1", "users-groups", "user-control", "great");

        assertSame(saved, out);
        verify(emailService).sendFeedbackEmail("u1", "users-groups", "user-control", "great");
        ArgumentCaptor<NotificationFeedback> cap = ArgumentCaptor.forClass(NotificationFeedback.class);
        verify(repository).save(cap.capture());
        assertEquals("great", cap.getValue().getFeedbackText());
    }

    @Test
    void submitFeedback_existingWithBlankText_updatesSendsEmailAndSaves() {
        NotificationFeedback existing = new NotificationFeedback();
        existing.setId(2L);
        existing.setFeedbackText("");
        when(repository.findByUserIdAndSourceAndNotificationType("u1", "s", "t"))
                .thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);

        service.submitFeedback("u1", "s", "t", "new text");

        assertEquals("new text", existing.getFeedbackText());
        verify(emailService).sendFeedbackEmail("u1", "s", "t", "new text");
        verify(repository).save(existing);
    }

    @Test
    void submitFeedback_existingWithNonBlankText_returnsWithoutEmailOrSave() {
        NotificationFeedback existing = new NotificationFeedback();
        existing.setFeedbackText("already");
        when(repository.findByUserIdAndSourceAndNotificationType("u1", "s", "t"))
                .thenReturn(Optional.of(existing));

        NotificationFeedback out = service.submitFeedback("u1", "s", "t", "ignored");

        assertSame(existing, out);
        verify(emailService, never()).sendFeedbackEmail(anyString(), anyString(), anyString(), anyString());
        verify(repository, never()).save(any());
    }

    @Test
    void hasFeedback_trueWhenStoredTextNonBlank() {
        NotificationFeedback f = new NotificationFeedback();
        f.setFeedbackText("x");
        when(repository.findByUserIdAndSourceAndNotificationType("u1", "s", "t"))
                .thenReturn(Optional.of(f));
        assertTrue(service.hasFeedback("u1", "s", "t"));
    }

    @Test
    void hasFeedback_falseWhenMissingOrBlank() {
        when(repository.findByUserIdAndSourceAndNotificationType("u1", "s", "t"))
                .thenReturn(Optional.empty());
        assertFalse(service.hasFeedback("u1", "s", "t"));
    }

    @Test
    void getFeedbackKeysForUser_skipsBlankFeedback() {
        NotificationFeedback a = new NotificationFeedback();
        a.setSource("s1");
        a.setNotificationType("t1");
        a.setFeedbackText("ok");
        NotificationFeedback b = new NotificationFeedback();
        b.setSource("s2");
        b.setNotificationType("t2");
        b.setFeedbackText("   ");
        when(repository.findByUserId("u1")).thenReturn(List.of(a, b));

        Set<String> keys = service.getFeedbackKeysForUser("u1");

        assertEquals(Set.of("s1:t1"), keys);
    }

    @Test
    void getAllFeedbackKeys_mapsSourceAndType() {
        NotificationFeedback f = new NotificationFeedback();
        f.setSource("app-access");
        f.setNotificationType("oauth-high-risk");
        when(repository.findAllWithFeedback()).thenReturn(List.of(f));

        assertEquals(Set.of("app-access:oauth-high-risk"), service.getAllFeedbackKeys());
    }
}
