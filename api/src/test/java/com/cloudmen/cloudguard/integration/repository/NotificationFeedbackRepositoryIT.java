package com.cloudmen.cloudguard.integration.repository;

import com.cloudmen.cloudguard.domain.model.feedback.NotificationFeedback;
import com.cloudmen.cloudguard.repository.NotificationFeedbackRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link NotificationFeedbackRepository}.
 *
 * <p>The custom JPQL on {@code findAllWithFeedback()} uses {@code LENGTH(TRIM(...))}
 * — worth verifying on real MySQL.
 */
class NotificationFeedbackRepositoryIT extends AbstractRepositoryIT {

    @Autowired
    private NotificationFeedbackRepository notificationFeedbackRepository;

    @BeforeEach
    void cleanSlate() {
        notificationFeedbackRepository.deleteAll();
    }

    @Test
    @DisplayName("findAllWithFeedback returns only rows with non-blank feedback text")
    void findAllWithFeedback_excludesNullEmptyAndWhitespaceOnly() {
        notificationFeedbackRepository.save(feedback("u1", "src", "type-a", "real comment"));
        notificationFeedbackRepository.save(feedback("u2", "src", "type-b", null));
        notificationFeedbackRepository.save(feedback("u3", "src", "type-c", ""));
        notificationFeedbackRepository.save(feedback("u4", "src", "type-d", "   "));
        notificationFeedbackRepository.save(feedback("u5", "src", "type-e", "  trimmed  "));

        List<NotificationFeedback> withText = notificationFeedbackRepository.findAllWithFeedback();

        assertThat(withText)
                .extracting(NotificationFeedback::getUserId)
                .containsExactlyInAnyOrder("u1", "u5");
    }

    @Test
    @DisplayName("findByUserIdAndSourceAndNotificationType resolves the composite natural key")
    void findByUserIdAndSourceAndNotificationType_returnsUniqueRow() {
        notificationFeedbackRepository.save(feedback("user-x", "google", "device-lock", "ok"));
        notificationFeedbackRepository.save(feedback("user-x", "google", "other", "x"));

        Optional<NotificationFeedback> found =
                notificationFeedbackRepository.findByUserIdAndSourceAndNotificationType(
                        "user-x", "google", "device-lock");

        assertThat(found).isPresent().get().extracting(NotificationFeedback::getFeedbackText).isEqualTo("ok");
    }

    @Test
    @DisplayName("findByUserId returns all feedback rows for that user")
    void findByUserId_returnsAllForUser() {
        notificationFeedbackRepository.save(feedback("alice", "s1", "t1", "a"));
        notificationFeedbackRepository.save(feedback("alice", "s2", "t2", "b"));
        notificationFeedbackRepository.save(feedback("bob", "s1", "t1", "c"));

        assertThat(notificationFeedbackRepository.findByUserId("alice")).hasSize(2);
    }

    private static NotificationFeedback feedback(
            String userId, String source, String type, String text) {
        NotificationFeedback f = new NotificationFeedback();
        f.setUserId(userId);
        f.setSource(source);
        f.setNotificationType(type);
        f.setFeedbackText(text);
        f.setCreatedAt(LocalDateTime.of(2026, 5, 11, 10, 0));
        return f;
    }
}
