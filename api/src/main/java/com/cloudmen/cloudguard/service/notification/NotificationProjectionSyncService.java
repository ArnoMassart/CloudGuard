package com.cloudmen.cloudguard.service.notification;

import com.cloudmen.cloudguard.domain.model.User;
import com.cloudmen.cloudguard.domain.model.notification.NotificationInstance;
import com.cloudmen.cloudguard.domain.model.notification.NotificationInstanceStatus;
import com.cloudmen.cloudguard.domain.model.notification.NotificationSeverity;
import com.cloudmen.cloudguard.dto.notifications.NotificationDto;
import com.cloudmen.cloudguard.repository.NotificationInstanceRepository;
import com.cloudmen.cloudguard.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class NotificationProjectionSyncService {

    private final NotificationInstanceRepository instanceRepository;
    private final NotificationAggregationService aggregationService;
    private final UserRepository userRepository;

    public NotificationProjectionSyncService(
            NotificationInstanceRepository instanceRepository,
            NotificationAggregationService aggregationService,
            UserRepository userRepository) {
        this.instanceRepository = instanceRepository;
        this.aggregationService = aggregationService;
        this.userRepository = userRepository;
    }

    /**
     * Reconcile org-wide notification rows with the current aggregation snapshot (Google/workspace truth).
     * Uses the lexicographically first user in the org (by id) as the Google API actor.
     */
    @Transactional
    public void syncOrganization(Long organizationId) {
        Optional<User> actorOpt = userRepository.findFirstByOrganizationIdOrderByIdAsc(organizationId);
        if (actorOpt.isEmpty()) {
            return;
        }
        User actor = actorOpt.get();
        String lang = actor.getLanguage() != null ? actor.getLanguage() : "nl";
        Locale locale = Locale.forLanguageTag(lang.replace('_', '-'));

        List<NotificationDto> snapshot =
                aggregationService.buildActiveSnapshot(actor.getEmail(), locale);

        List<NotificationInstance> existing = instanceRepository.findByOrganizationId(organizationId);
        Map<String, NotificationInstance> byKey = new HashMap<>();
        for (NotificationInstance row : existing) {
            byKey.put(row.getSource() + ":" + row.getNotificationType(), row);
        }

        Set<String> snapshotKeys = new HashSet<>();
        LocalDateTime now = LocalDateTime.now();

        for (NotificationDto dto : snapshot) {
            String key = dto.source() + ":" + dto.notificationType();
            snapshotKeys.add(key);

            NotificationInstance row = byKey.get(key);
            if (row == null) {
                row = new NotificationInstance();
                row.setOrganizationId(organizationId);
                row.setSource(dto.source());
                row.setNotificationType(dto.notificationType());
                row.setFirstObservedAt(now);
                row.setStatus(NotificationInstanceStatus.ACTIVE);
                byKey.put(key, row);
            } else if (row.getStatus() == NotificationInstanceStatus.SOLVED) {
                row.setStatus(NotificationInstanceStatus.ACTIVE);
                row.setSolvedAt(null);
            }

            row.setSeverity(NotificationSeverity.fromDtoString(dto.severity()));
            row.setTitle(dto.title());
            row.setDescription(dto.description());
            row.setRecommendedActions(
                    dto.recommendedActions() != null ? dto.recommendedActions() : List.of());
            row.setSourceLabel(dto.sourceLabel());
            row.setSourceRoute(dto.sourceRoute());
            row.setLastObservedAt(now);

            instanceRepository.save(row);
        }

        for (NotificationInstance row : existing) {
            if (row.getStatus() != NotificationInstanceStatus.ACTIVE) {
                continue;
            }
            String key = row.getSource() + ":" + row.getNotificationType();
            if (!snapshotKeys.contains(key)) {
                row.setStatus(NotificationInstanceStatus.SOLVED);
                row.setSolvedAt(now);
                instanceRepository.save(row);
            }
        }
    }
}
