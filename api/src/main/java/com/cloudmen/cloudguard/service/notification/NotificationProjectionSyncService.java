package com.cloudmen.cloudguard.service.notification;

import com.cloudmen.cloudguard.domain.model.User;
import com.cloudmen.cloudguard.domain.model.UserRole;
import com.cloudmen.cloudguard.domain.model.notification.NotificationInstance;
import com.cloudmen.cloudguard.domain.model.notification.NotificationInstanceStatus;
import com.cloudmen.cloudguard.domain.model.notification.NotificationSeverity;
import com.cloudmen.cloudguard.dto.notifications.NotificationDto;
import com.cloudmen.cloudguard.repository.NotificationInstanceRepository;
import com.cloudmen.cloudguard.repository.UserRepository;
import com.cloudmen.cloudguard.service.preference.PreferenceToNotificationMapping;
import com.cloudmen.cloudguard.service.preference.UserSecurityPreferenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class NotificationProjectionSyncService {

    private static final Logger log = LoggerFactory.getLogger(NotificationProjectionSyncService.class);

    private final NotificationInstanceRepository instanceRepository;
    private final NotificationAggregationService aggregationService;
    private final UserRepository userRepository;
    private final UserSecurityPreferenceService preferenceService;

    public NotificationProjectionSyncService(
            NotificationInstanceRepository instanceRepository,
            NotificationAggregationService aggregationService,
            UserRepository userRepository,
            UserSecurityPreferenceService preferenceService) {
        this.instanceRepository = instanceRepository;
        this.aggregationService = aggregationService;
        this.userRepository = userRepository;
        this.preferenceService = preferenceService;
    }

    /**
     * Reconcile org-wide notification rows with the current aggregation snapshot (Google/workspace truth).
     * Uses a {@link UserRole#SUPER_ADMIN} member of the org (lowest id) as the Google API actor so the snapshot
     * reflects full admin visibility. If none exists, sync is skipped.
     */
    @Transactional
    public void syncOrganization(Long organizationId) {
        List<User> superAdmins =
                userRepository.findByOrganizationIdAndRoleOrderByIdAsc(organizationId, UserRole.SUPER_ADMIN);
        if (superAdmins.isEmpty()) {
            log.warn(
                    "Skipping notification projection sync for organization {}: no user with SUPER_ADMIN role",
                    organizationId);
            return;
        }
        User actor = superAdmins.get(0);
        String actorEmail = actor.getEmail();
        String lang = actor.getLanguage() != null ? actor.getLanguage() : "nl";
        Locale locale = Locale.forLanguageTag(lang.replace('_', '-'));

        Set<String> disabledPreferenceKeys = preferenceService.getDisabledPreferenceKeys(actorEmail);

        List<NotificationDto> snapshot =
                aggregationService.buildActiveSnapshot(actorEmail, locale);

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

            boolean disabledInPreferences =
                    PreferenceToNotificationMapping.isDisabledByPreference(
                            dto.source(), dto.notificationType(), disabledPreferenceKeys);

            NotificationInstance row = byKey.get(key);
            if (row == null) {
                row = new NotificationInstance();
                row.setOrganizationId(organizationId);
                row.setSource(dto.source());
                row.setNotificationType(dto.notificationType());
                row.setFirstObservedAt(now);
                row.setStatus(
                        disabledInPreferences
                                ? NotificationInstanceStatus.DISABLED
                                : NotificationInstanceStatus.ACTIVE);
                byKey.put(key, row);
            } else if (disabledInPreferences) {
                row.setStatus(NotificationInstanceStatus.DISABLED);
                row.setSolvedAt(null);
            } else {
                if (row.getStatus() == NotificationInstanceStatus.SOLVED
                        || row.getStatus() == NotificationInstanceStatus.DISABLED) {
                    row.setStatus(NotificationInstanceStatus.ACTIVE);
                    row.setSolvedAt(null);
                }
            }

            row.setSeverity(NotificationSeverity.fromDtoString(dto.severity()));
            row.setTitle(dto.title());
            row.setDescription(dto.description());
            row.setRecommendedActions(
                    dto.recommendedActions() != null
                            ? new ArrayList<>(dto.recommendedActions())
                            : new ArrayList<>());
            row.setSourceLabel(dto.sourceLabel());
            row.setSourceRoute(dto.sourceRoute());
            row.setLastObservedAt(now);

            instanceRepository.save(row);
        }

        for (NotificationInstance row : existing) {
            String key = row.getSource() + ":" + row.getNotificationType();
            if (snapshotKeys.contains(key)) {
                continue;
            }
            boolean disabledInPreferences =
                    PreferenceToNotificationMapping.isDisabledByPreference(
                            row.getSource(), row.getNotificationType(), disabledPreferenceKeys);
            if (disabledInPreferences) {
                row.setStatus(NotificationInstanceStatus.DISABLED);
                row.setSolvedAt(null);
                instanceRepository.save(row);
            } else if (row.getStatus() == NotificationInstanceStatus.ACTIVE) {
                row.setStatus(NotificationInstanceStatus.SOLVED);
                row.setSolvedAt(now);
                instanceRepository.save(row);
            }
        }
    }
}
