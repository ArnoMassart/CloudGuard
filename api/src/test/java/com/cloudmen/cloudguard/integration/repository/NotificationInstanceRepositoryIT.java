package com.cloudmen.cloudguard.integration.repository;

import com.cloudmen.cloudguard.domain.model.notification.NotificationInstance;
import com.cloudmen.cloudguard.domain.model.notification.NotificationInstanceStatus;
import com.cloudmen.cloudguard.domain.model.notification.NotificationSeverity;
import com.cloudmen.cloudguard.repository.NotificationInstanceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link NotificationInstanceRepository} against real MySQL.
 *
 * <p>Focuses on behaviour that cannot be validated with mocks alone:
 * <ul>
 *   <li>Native {@code INSERT … ON DUPLICATE KEY UPDATE} used by notification projection sync.</li>
 *   <li>JPQL bulk {@code DELETE} for solved-notification retention cleanup.</li>
 * </ul>
 */
class NotificationInstanceRepositoryIT extends AbstractRepositoryIT {

    @Autowired
    private NotificationInstanceRepository notificationInstanceRepository;

    @BeforeEach
    void cleanSlate() {
        notificationInstanceRepository.deleteAll();
    }

    @Test
    @DisplayName("insertIfMissing inserts a row that findByOrganizationIdAndSourceAndNotificationType can load")
    void insertIfMissing_createsRowReadableViaFinder() {
        LocalDateTime now = LocalDateTime.of(2026, 5, 11, 12, 0);
        int inserted =
                notificationInstanceRepository.insertIfMissing(
                        42L,
                        "src-google",
                        "drive-external",
                        NotificationInstanceStatus.ACTIVE.name(),
                        NotificationSeverity.WARNING.name(),
                        "Title",
                        "Desc",
                        "Google Workspace",
                        "/shared-drives",
                        now);

        assertThat(inserted).isEqualTo(1);

        Optional<NotificationInstance> found =
                notificationInstanceRepository.findByOrganizationIdAndSourceAndNotificationType(
                        42L, "src-google", "drive-external");

        assertThat(found)
                .isPresent()
                .get()
                .satisfies(
                        n -> {
                            assertThat(n.getOrganizationId()).isEqualTo(42L);
                            assertThat(n.getStatus()).isEqualTo(NotificationInstanceStatus.ACTIVE);
                            assertThat(n.getSeverity()).isEqualTo(NotificationSeverity.WARNING);
                            assertThat(n.getTitle()).isEqualTo("Title");
                        });
    }

    @Test
    @DisplayName("insertIfMissing second call with same unique key does not create a duplicate row")
    void insertIfMissing_onDuplicateDoesNotDuplicateRow() {
        LocalDateTime now = LocalDateTime.of(2026, 5, 11, 12, 0);
        notificationInstanceRepository.insertIfMissing(
                1L,
                "same-source",
                "same-type",
                NotificationInstanceStatus.ACTIVE.name(),
                NotificationSeverity.INFO.name(),
                "A",
                null,
                "lbl",
                "/route",
                now);
        notificationInstanceRepository.insertIfMissing(
                1L,
                "same-source",
                "same-type",
                NotificationInstanceStatus.ACTIVE.name(),
                NotificationSeverity.CRITICAL.name(),
                "B",
                "changed",
                "lbl2",
                "/route2",
                now);

        List<NotificationInstance> all = notificationInstanceRepository.findByOrganizationId(1L);
        assertThat(all).hasSize(1);
    }

    @Test
    @DisplayName("deleteByStatusAndSolvedAtBefore removes only solved rows older than the cutoff")
    void deleteByStatusAndSolvedAtBefore_removesStaleSolvedOnly() {
        LocalDateTime cutoff = LocalDateTime.of(2026, 6, 1, 0, 0);

        NotificationInstance oldSolved = solved("old-solved", cutoff.minusDays(10));
        // Still SOLVED but solved_at is on or after the cutoff — must not be deleted.
        NotificationInstance recentSolved = solved("recent-solved", cutoff.plusMinutes(30));
        NotificationInstance active = active("still-active");

        notificationInstanceRepository.saveAll(List.of(oldSolved, recentSolved, active));

        int deleted =
                notificationInstanceRepository.deleteByStatusAndSolvedAtBefore(
                        NotificationInstanceStatus.SOLVED, cutoff);

        assertThat(deleted).isEqualTo(1);

        List<NotificationInstance> remaining = notificationInstanceRepository.findAll();
        assertThat(remaining)
                .extracting(NotificationInstance::getNotificationType)
                .containsExactlyInAnyOrder("recent-solved", "still-active");
    }

    @Test
    @DisplayName("findByOrganizationIdAndStatusAndSeverity filters correctly")
    void findByOrganizationIdAndStatusAndSeverity_filters() {
        NotificationInstance n1 = active("t1");
        n1.setOrganizationId(7L);
        n1.setSeverity(NotificationSeverity.HIGH);

        NotificationInstance n2 = active("t2");
        n2.setOrganizationId(7L);
        n2.setSeverity(NotificationSeverity.LOW);

        notificationInstanceRepository.saveAll(List.of(n1, n2));

        List<NotificationInstance> high =
                notificationInstanceRepository.findByOrganizationIdAndStatusAndSeverity(
                        7L, NotificationInstanceStatus.ACTIVE, NotificationSeverity.HIGH);

        assertThat(high).hasSize(1);
        assertThat(high.get(0).getNotificationType()).isEqualTo("t1");
    }

    private static NotificationInstance active(String type) {
        NotificationInstance n = new NotificationInstance();
        n.setOrganizationId(1L);
        n.setSource("s");
        n.setNotificationType(type);
        n.setStatus(NotificationInstanceStatus.ACTIVE);
        n.setSeverity(NotificationSeverity.INFO);
        return n;
    }

    private static NotificationInstance solved(String type, LocalDateTime solvedAt) {
        NotificationInstance n = active(type);
        n.setStatus(NotificationInstanceStatus.SOLVED);
        n.setSolvedAt(solvedAt);
        return n;
    }
}
