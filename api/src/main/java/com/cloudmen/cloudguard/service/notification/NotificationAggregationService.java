package com.cloudmen.cloudguard.service.notification;

import com.cloudmen.cloudguard.configuration.NotificationProjectionProperties;
import com.cloudmen.cloudguard.domain.model.Organization;
import com.cloudmen.cloudguard.domain.model.User;
import com.cloudmen.cloudguard.domain.model.UserRole;
import com.cloudmen.cloudguard.domain.model.notification.NotificationInstance;
import com.cloudmen.cloudguard.domain.model.notification.NotificationInstanceStatus;
import com.cloudmen.cloudguard.domain.model.notification.NotificationSeverity;
import com.cloudmen.cloudguard.domain.model.DnsRecordImportance;
import com.cloudmen.cloudguard.domain.model.DnsRecordStatus;
import com.cloudmen.cloudguard.dto.devices.DeviceOverviewResponse;
import com.cloudmen.cloudguard.dto.dns.DnsRecordDto;
import com.cloudmen.cloudguard.dto.dns.DnsRecordResponseDto;
import com.cloudmen.cloudguard.dto.domain.DomainDto;
import com.cloudmen.cloudguard.dto.drives.SharedDriveOverviewResponse;
import com.cloudmen.cloudguard.dto.groups.GroupOverviewResponse;
import com.cloudmen.cloudguard.dto.notifications.NotificationDto;
import com.cloudmen.cloudguard.dto.notifications.NotificationsResponse;
import com.cloudmen.cloudguard.dto.oauth.OAuthOverviewResponse;
import com.cloudmen.cloudguard.dto.apppasswords.AppPasswordOverviewResponse;
import com.cloudmen.cloudguard.dto.password.PasswordSettingsDto;
import com.cloudmen.cloudguard.dto.users.UserOverviewResponse;
import com.cloudmen.cloudguard.service.*;
import com.cloudmen.cloudguard.service.dns.DnsRecordsService;
import com.cloudmen.cloudguard.repository.NotificationInstanceRepository;
import com.cloudmen.cloudguard.repository.OrganizationRepository;
import com.cloudmen.cloudguard.repository.UserRepository;
import com.cloudmen.cloudguard.service.preference.PreferenceToNotificationMapping;
import com.cloudmen.cloudguard.service.preference.UserSecurityPreferenceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Supplier;

import static com.cloudmen.cloudguard.utility.GoogleServiceHelperMethods.hasAccessToModule;

@Service
public class NotificationAggregationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationAggregationService.class);

    private static final Map<String, String> DNS_TITLES = Map.of(
            "SPF", "SPF Record",
            "DKIM", "DKIM",
            "DMARC", "DMARC",
            "MX", "MX Records",
            "DNSSEC", "DNSSEC",
            "CAA", "CAA Records"
    );

    private static final Set<String> NOTIFICATION_TYPES_WITH_DETAILS = Set.of(
            "user-control", "group-external", "oauth-high-risk", "drive-orphan", "drive-external",
            "drive-outside-domain", "drive-non-member-access",
            "device-lockscreen", "device-encryption", "device-os", "device-integrity",
            "password-2sv-not-enforced", "password-weak-length", "password-strong-not-required",
            "password-never-expires", "password-admins-no-security-keys"
    );
    private static final Map<String, String> SOURCE_LABEL_KEYS = Map.of(
            "domain-dns", "notifications.dns.label",
            "users-groups", "notifications.users_groups.label",
            "shared-drives", "notifications.drives.label",
            "devices", "notifications.devices.label",
            "app-access", "notifications.app_access.label",
            "app-passwords", "notifications.app_passwords.label",
            "password-settings", "notifications.password_settings.label"
    );

    private final GoogleDomainService domainService;
    private final DnsRecordsService dnsRecordsService;
    private final GoogleUsersService usersService;
    private final GoogleSharedDriveService driveService;
    private final GoogleDeviceService deviceService;
    private final AppPasswordsService appPasswordsService;
    private final GoogleGroupsService groupsService;
    private final GoogleOAuthService oAuthService;
    private final PasswordSettingsService passwordSettingsService;
    private final NotificationFeedbackService feedbackService;
    private final UserSecurityPreferenceService preferenceService;
    private final MessageSource messageSource;
    private final NotificationInstanceRepository notificationInstanceRepository;
    private final NotificationProjectionProperties notificationProjectionProperties;
    private final UserService userService;
    private final OrganizationRepository organizationRepository;

    public NotificationAggregationService(
            GoogleDomainService domainService,
            DnsRecordsService dnsRecordsService,
            GoogleUsersService usersService,
            GoogleSharedDriveService driveService,
            GoogleDeviceService deviceService,
            AppPasswordsService appPasswordsService,
            GoogleGroupsService groupsService,
            GoogleOAuthService oAuthService,
            MessageSource messageSource,
            PasswordSettingsService passwordSettingsService,
            NotificationFeedbackService feedbackService,
            UserSecurityPreferenceService preferenceService,
            NotificationInstanceRepository notificationInstanceRepository,
            NotificationProjectionProperties notificationProjectionProperties,
            UserService userService,
            OrganizationRepository organizationRepository) {
        this.domainService = domainService;
        this.dnsRecordsService = dnsRecordsService;
        this.usersService = usersService;
        this.driveService = driveService;
        this.deviceService = deviceService;
        this.appPasswordsService = appPasswordsService;
        this.groupsService = groupsService;
        this.oAuthService = oAuthService;
        this.passwordSettingsService = passwordSettingsService;
        this.feedbackService = feedbackService;
        this.preferenceService = preferenceService;
        this.messageSource = messageSource;
        this.notificationInstanceRepository = notificationInstanceRepository;
        this.notificationProjectionProperties = notificationProjectionProperties;
        this.userService = userService;
        this.organizationRepository = organizationRepository;
    }

    /** Live aggregation snapshot (used by the scheduled sync job). */
    public List<NotificationDto> buildActiveSnapshot(String userEmail, Locale locale) {
        Set<String> disabled = preferenceService.getDisabledPreferenceKeys(userEmail);

        User user = userService.findByEmailOptional(userEmail).orElse(null);

        List<UserRole> roles = user != null ? user.getRoles() : null;
        return aggregateActive(userEmail, locale, disabled, roles);
    }

    public NotificationsResponse getNotifications(String userId, Locale locale) {
        User user = userService.findByEmailOptional(userId).orElse(null);
        Set<String> disabledPreferenceKeys = preferenceService.getDisabledPreferenceKeys(userId);
        List<UserRole> viewerRoles = user != null && user.getRoles() != null ? user.getRoles() : null;

        List<NotificationDto> active = resolveActiveNotifications(user, userId, locale, disabledPreferenceKeys);
        active = excludeNotificationsMarkedDisabledInDb(user, active);
        if (viewerRoles != null) {
            active =
                    active.stream()
                            .filter(
                                    n -> NotificationSourceViewerRoles.isSourceVisibleToRoles(
                                            n.source(), viewerRoles))
                            .toList();
        }

        Set<String> feedbackKeys = feedbackService.getAllFeedbackKeys();

        List<NotificationDto> filtered = active.stream()
                .filter(n -> !isHiddenByPreference(n.source(), n.notificationType(), disabledPreferenceKeys))
                .map(n -> withStatus(n, feedbackKeys))
                .toList();

        List<NotificationDto> solved = resolveSolvedNotifications(user, viewerRoles, locale);
        List<NotificationDto> solvedFiltered =
                solved.stream().map(n -> withStatus(n, feedbackKeys)).toList();

        String lastNotificationSyncAt = null;
        if (user != null && user.getOrganizationId() != null) {
            lastNotificationSyncAt =
                    organizationRepository
                            .findById(user.getOrganizationId())
                            .map(Organization::getLastNotificationSyncAt)
                            .map(ldt -> ldt.atZone(ZoneId.systemDefault()).toInstant().toString())
                            .orElse(null);
        }

        return new NotificationsResponse(filtered, solvedFiltered, lastNotificationSyncAt);
    }

    /**
     * Solved projection rows (issue no longer detected); only when org has synced notification instances.
     */
    private List<NotificationDto> resolveSolvedNotifications(
            User user, List<UserRole> viewerRoles, Locale locale) {
        if (!notificationProjectionProperties.isReadEnabled()) {
            return List.of();
        }
        if (user == null || user.getOrganizationId() == null) {
            return List.of();
        }
        Long orgId = user.getOrganizationId();
        if (!notificationInstanceRepository.existsByOrganizationId(orgId)) {
            return List.of();
        }
        List<NotificationInstance> solvedRows =
                notificationInstanceRepository.findByOrganizationIdAndStatus(
                        orgId, NotificationInstanceStatus.SOLVED);
        solvedRows.sort(
                Comparator.comparing(
                        NotificationInstance::getSolvedAt, Comparator.nullsLast(Comparator.reverseOrder())));
        List<NotificationDto> dtos = solvedRows.stream().map(row -> toDtoFromProjection(row, locale)).toList();
        if (viewerRoles == null) {
            return dtos;
        }
        return dtos.stream()
                .filter(n -> NotificationSourceViewerRoles.isSourceVisibleToRoles(n.source(), viewerRoles))
                .toList();
    }

    /**
     * Drops notifications that are stored as {@link NotificationInstanceStatus#DISABLED} for this org.
     * Needed when the live aggregation path is used: it does not read {@code tbl_notifications} status,
     * but sync may still have marked rows DISABLED.
     */
    private List<NotificationDto> excludeNotificationsMarkedDisabledInDb(User user, List<NotificationDto> notifications) {
        if (user == null || user.getOrganizationId() == null || notifications.isEmpty()) {
            return notifications;
        }
        List<NotificationInstance> disabledRows =
                notificationInstanceRepository.findByOrganizationIdAndStatus(
                        user.getOrganizationId(), NotificationInstanceStatus.DISABLED);
        if (disabledRows.isEmpty()) {
            return notifications;
        }
        Set<String> disabledKeys = new HashSet<>();
        for (NotificationInstance row : disabledRows) {
            disabledKeys.add(row.getSource() + ":" + row.getNotificationType());
        }
        return notifications.stream()
                .filter(n -> !disabledKeys.contains(n.source() + ":" + n.notificationType()))
                .toList();
    }

    private List<NotificationDto> resolveActiveNotifications(
            User user, String userId, Locale locale, Set<String> disabledPreferenceKeys) {
        List<UserRole> roles = user != null ? user.getRoles() : null;

        if (!notificationProjectionProperties.isReadEnabled()) {
            return aggregateActive(userId, locale, disabledPreferenceKeys, roles);
        }
        Long orgId = user != null ? user.getOrganizationId() : null;
        if (orgId == null) {
            return aggregateActive(userId, locale, disabledPreferenceKeys, roles);
        }
        boolean everSynced = notificationInstanceRepository.existsByOrganizationId(orgId);
        if (!everSynced) {
            List<NotificationDto> live = aggregateActive(userId, locale, disabledPreferenceKeys, roles);
            persistAggregatedNotifications(orgId, live);
            return live;
        }
        List<NotificationInstance> projected =
                notificationInstanceRepository.findByOrganizationIdAndStatus(
                        orgId, NotificationInstanceStatus.ACTIVE);
        return projected.stream().map(row -> toDtoFromProjection(row, locale)).toList();
    }

    /**
     * Persists live-aggregated DTOs so the projection table is populated immediately,
     * without waiting for the daily sync job.
     */
    private void persistAggregatedNotifications(Long orgId, List<NotificationDto> dtos) {
        for (NotificationDto dto : dtos) {
            boolean exists = notificationInstanceRepository
                    .findByOrganizationIdAndSourceAndNotificationType(orgId, dto.source(), dto.notificationType())
                    .isPresent();
            if (exists) {
                continue;
            }
            NotificationInstance row = new NotificationInstance();
            row.setOrganizationId(orgId);
            row.setSource(dto.source());
            row.setNotificationType(dto.notificationType());
            row.setStatus(NotificationInstanceStatus.ACTIVE);
            row.setSeverity(NotificationSeverity.fromDtoString(dto.severity()));
            row.setTitle(dto.title());
            row.setDescription(dto.description());
            row.setRecommendedActions(
                    dto.recommendedActions() != null
                            ? new ArrayList<>(dto.recommendedActions())
                            : new ArrayList<>());
            row.setSourceLabel(dto.sourceLabel());
            row.setSourceRoute(dto.sourceRoute());
            notificationInstanceRepository.save(row);
        }
    }

    private NotificationDto toDtoFromProjection(NotificationInstance f, Locale locale) {
        List<String> actions =
                f.getRecommendedActions() != null ? f.getRecommendedActions() : List.of();
        boolean supportsDetails = NOTIFICATION_TYPES_WITH_DETAILS.contains(f.getNotificationType());
        String severityStr = NotificationSeverity.toDtoString(f.getSeverity());
        String createdAtIso =
                f.getCreatedAt() == null
                        ? null
                        : f.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant().toString();
        String localizedTitle = localizeProjectedTitle(f, locale);
        String localizedDescription = localizeProjectedDescription(f, locale);
        List<String> localizedActions = localizeProjectedActions(f, locale, actions);
        String localizedSourceLabel = localizeProjectedSourceLabel(f, locale);
        return new NotificationDto(
                "nf-" + f.getId(),
                severityStr,
                localizedTitle,
                localizedDescription,
                localizedActions,
                f.getNotificationType(),
                f.getSource(),
                localizedSourceLabel,
                f.getSourceRoute(),
                false,
                supportsDetails,
                createdAtIso);
    }

    private String localizeProjectedTitle(NotificationInstance row, Locale locale) {
        return switch (row.getNotificationType()) {
            case "dns-critical" -> messageSource.getMessage("notifications.dns.critical.title", null, locale);
            case "dns-attention" -> messageSource.getMessage("notifications.dns.attention.title", null, locale);
            case "user-control" -> messageSource.getMessage("notifications.users.without_2fa.title", null, locale);
            case "user-activity" -> isInactiveRecentVariant(row.getDescription())
                    ? messageSource.getMessage("notifications.users.inactive_recent.title", null, locale)
                    : messageSource.getMessage("notifications.users.active_no_login.title", null, locale);
            case "group-external" -> messageSource.getMessage("notifications.groups.title", null, locale);
            case "drive-orphan" -> messageSource.getMessage("notifications.drives.orphan.title", null, locale);
            case "drive-external" -> messageSource.getMessage("notifications.drives.external.title", null, locale);
            case "drive-outside-domain" -> messageSource.getMessage("notifications.drives.outside_domain.title", null, locale);
            case "drive-non-member-access" -> messageSource.getMessage("notifications.drives.non_member_access.title", null, locale);
            case "device-lockscreen" -> messageSource.getMessage("notifications.devices.lock.title", null, locale);
            case "device-encryption" -> messageSource.getMessage("notifications.devices.enc.title", null, locale);
            case "device-os" -> messageSource.getMessage("notifications.devices.os.title", null, locale);
            case "device-integrity" -> messageSource.getMessage("notifications.devices.int.title", null, locale);
            case "oauth-high-risk" -> messageSource.getMessage("notifications.app_access.title", null, locale);
            case "app-password" -> messageSource.getMessage("notifications.app_passwords.title", null, locale);
            case "password-2sv-not-enforced" ->
                    messageSource.getMessage("notifications.password_settings.without_2SV.title", null, locale);
            case "password-weak-length" ->
                    messageSource.getMessage("notifications.password_settings.weak_length.title", null, locale);
            case "password-strong-not-required" ->
                    messageSource.getMessage("notifications.password_settings.no_strong.title", null, locale);
            case "password-never-expires" ->
                    messageSource.getMessage("notifications.password_settings.no_expiry.title", null, locale);
            case "password-admins-no-security-keys" ->
                    messageSource.getMessage("notifications.password_settings.without_keys.title", null, locale);
            default -> row.getTitle();
        };
    }

    private String localizeProjectedDescription(NotificationInstance row, Locale locale) {
        if ("dns-critical".equals(row.getNotificationType()) || "dns-attention".equals(row.getNotificationType())) {
            return row.getDescription();
        }

        Long count = extractFirstLong(row.getDescription());
        if (count == null) {
            return row.getDescription();
        }

        return switch (row.getNotificationType()) {
            case "user-control" -> messageSource.getMessage(
                    "notifications.users.without_2fa.description", new Object[] {count}, locale);
            case "user-activity" -> {
                String key = isInactiveRecentVariant(row.getDescription())
                        ? "notifications.users.inactive_recent.description"
                        : "notifications.users.active_no_login.description";
                yield messageSource.getMessage(key, new Object[] {count}, locale);
            }
            case "group-external" -> messageSource.getMessage(
                    "notifications.groups.description", new Object[] {count}, locale);
            case "drive-orphan" -> messageSource.getMessage(
                    "notifications.drives.orphan.description", new Object[] {count}, locale);
            case "drive-external" -> messageSource.getMessage(
                    "notifications.drives.external.description", new Object[] {count}, locale);
            case "drive-outside-domain" -> messageSource.getMessage(
                    "notifications.drives.outside_domain.description", new Object[] {count}, locale);
            case "drive-non-member-access" -> messageSource.getMessage(
                    "notifications.drives.non_member_access.description", new Object[] {count}, locale);
            case "device-lockscreen" -> messageSource.getMessage(
                    "notifications.devices.lock.description", new Object[] {count}, locale);
            case "device-encryption" -> messageSource.getMessage(
                    "notifications.devices.enc.description", new Object[] {count}, locale);
            case "device-os" -> messageSource.getMessage(
                    "notifications.devices.os.description", new Object[] {count}, locale);
            case "device-integrity" -> messageSource.getMessage(
                    "notifications.devices.int.description", new Object[] {count}, locale);
            case "oauth-high-risk" -> messageSource.getMessage(
                    "notifications.app_access.description", new Object[] {count}, locale);
            case "app-password" -> messageSource.getMessage(
                    "notifications.app_passwords.description", new Object[] {count}, locale);
            case "password-2sv-not-enforced" -> messageSource.getMessage(
                    "notifications.password_settings.without_2SV.description", new Object[] {count}, locale);
            case "password-weak-length" -> messageSource.getMessage(
                    "notifications.password_settings.weak_length.description", new Object[] {count}, locale);
            case "password-strong-not-required" -> messageSource.getMessage(
                    "notifications.password_settings.no_strong.description", new Object[] {count}, locale);
            case "password-never-expires" -> messageSource.getMessage(
                    "notifications.password_settings.no_expiry.description", new Object[] {count}, locale);
            case "password-admins-no-security-keys" -> messageSource.getMessage(
                    "notifications.password_settings.without_keys.description", new Object[] {count}, locale);
            default -> row.getDescription();
        };
    }

    private List<String> localizeProjectedActions(
            NotificationInstance row, Locale locale, List<String> fallbackActions) {
        return switch (row.getNotificationType()) {
            case "dns-critical" -> List.of(messageSource.getMessage("notifications.dns.critical.actions", null, locale));
            case "dns-attention" -> List.of(messageSource.getMessage("notifications.dns.attention.actions", null, locale));
            case "user-control" -> List.of(
                    messageSource.getMessage("notifications.users.without_2fa.actions.1", null, locale),
                    messageSource.getMessage("notifications.users.without_2fa.actions.2", null, locale));
            case "user-activity" -> {
                String key = isInactiveRecentVariant(row.getDescription())
                        ? "notifications.users.inactive_recent.actions"
                        : "notifications.users.active_no_login.actions";
                yield List.of(messageSource.getMessage(key, null, locale));
            }
            case "group-external" -> List.of(messageSource.getMessage("notifications.groups.actions", null, locale));
            case "drive-orphan" -> List.of(messageSource.getMessage("notifications.drives.orphan.actions", null, locale));
            case "drive-external" -> List.of(messageSource.getMessage("notifications.drives.external.actions", null, locale));
            case "drive-outside-domain" ->
                    List.of(messageSource.getMessage("notifications.drives.outside_domain.actions", null, locale));
            case "drive-non-member-access" ->
                    List.of(messageSource.getMessage("notifications.drives.non_member_access.actions", null, locale));
            case "device-lockscreen" -> List.of(messageSource.getMessage("notifications.devices.lock.actions", null, locale));
            case "device-encryption" -> List.of(messageSource.getMessage("notifications.devices.enc.actions", null, locale));
            case "device-os" -> List.of(messageSource.getMessage("notifications.devices.os.actions", null, locale));
            case "device-integrity" -> List.of(messageSource.getMessage("notifications.devices.int.actions", null, locale));
            case "oauth-high-risk" -> List.of(
                    messageSource.getMessage("notifications.app_access.actions.1", null, locale),
                    messageSource.getMessage("notifications.app_access.actions.2", null, locale));
            case "app-password" -> List.of(messageSource.getMessage("notifications.app_passwords.actions", null, locale));
            case "password-2sv-not-enforced" ->
                    List.of(messageSource.getMessage("notifications.password_settings.without_2SV.actions", null, locale));
            case "password-weak-length" ->
                    List.of(messageSource.getMessage("notifications.password_settings.weak_length.actions", null, locale));
            case "password-strong-not-required" ->
                    List.of(messageSource.getMessage("notifications.password_settings.no_strong.actions", null, locale));
            case "password-never-expires" ->
                    List.of(messageSource.getMessage("notifications.password_settings.no_expiry.actions", null, locale));
            case "password-admins-no-security-keys" ->
                    List.of(messageSource.getMessage("notifications.password_settings.without_keys.actions", null, locale));
            default -> fallbackActions;
        };
    }

    private String localizeProjectedSourceLabel(NotificationInstance row, Locale locale) {
        String key = SOURCE_LABEL_KEYS.get(row.getSource());
        if (key == null) {
            return row.getSourceLabel();
        }
        return messageSource.getMessage(key, null, locale);
    }

    private Long extractFirstLong(String text) {
        if (text == null) {
            return null;
        }
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+)").matcher(text);
        if (!m.find()) {
            return null;
        }
        try {
            return Long.parseLong(m.group(1));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private boolean isInactiveRecentVariant(String description) {
        if (description == null) {
            return false;
        }
        String normalized = description.toLowerCase(Locale.ROOT);
        return normalized.contains("inactive")
                || normalized.contains("inactieve")
                || normalized.contains("gedeactiveerd")
                || normalized.contains("deactivated");
    }

    private boolean isHiddenByPreference(String source, String notificationType, Set<String> disabledPreferenceKeys) {
        return PreferenceToNotificationMapping.isDisabledByPreference(source, notificationType, disabledPreferenceKeys);
    }

    public long getNotificationsCount(String userId) {
        NotificationsResponse response = getNotifications(userId, LocaleContextHolder.getLocale());
        return response.active().size();
    }

    public long getNotificationsCriticalCount(String userId) {
        NotificationsResponse response = getNotifications(userId, LocaleContextHolder.getLocale());
        return response.active().stream()
                .filter(n -> "critical".equals(n.severity()))
                .count();
    }

    public List<NotificationDto> getCriticalNotifications(String userId, Locale locale) {
        NotificationsResponse response = getNotifications(userId, locale);
        return response.active().stream().filter(n -> n.severity().equals("critical")).toList();
    }

    private NotificationDto withStatus(NotificationDto n, Set<String> feedbackKeys) {
        boolean hasReported = feedbackKeys.contains(n.source() + ":" + n.notificationType());
        return new NotificationDto(
                n.id(),
                n.severity(),
                n.title(),
                n.description(),
                n.recommendedActions(),
                n.notificationType(),
                n.source(),
                n.sourceLabel(),
                n.sourceRoute(),
                hasReported,
                n.supportsDetails(),
                n.createdAt());
    }

    private List<NotificationDto> aggregateActive(String adminEmail, Locale locale, Set<String> disabled, List<UserRole> roles) {
        List<NotificationDto> notifications = new ArrayList<>();
        int id = 0;

        UserOverviewResponse users = null;
        if (hasAccessToModule(roles, UserRole.USERS_GROUPS_VIEWER)) {
            users = safeGet(() -> usersService.getUsersPageOverview(adminEmail, disabled));
        }

        SharedDriveOverviewResponse drives = null;
        if (hasAccessToModule(roles, UserRole.SHARED_DRIVES_VIEWER)) {
        drives = safeGet(() -> driveService.getDrivesPageOverview(adminEmail, disabled));
        }

        DeviceOverviewResponse devices = null;
        if (hasAccessToModule(roles, UserRole.DEVICES_VIEWER)) {
            devices = safeGet(() -> deviceService.getDevicesPageOverview(adminEmail, disabled));
        }

        GroupOverviewResponse groups = null;
        if (hasAccessToModule(roles, UserRole.USERS_GROUPS_VIEWER)) {
            groups = safeGet(() -> groupsService.getGroupsOverview(adminEmail, disabled));
        }

        OAuthOverviewResponse oAuth = null;
        if (hasAccessToModule(roles, UserRole.APP_ACCESS_VIEWER)) {
            oAuth = safeGet(() -> oAuthService.getOAuthPageOverview(adminEmail, disabled));
        }

        AppPasswordOverviewResponse appPasswords = null;
        if (hasAccessToModule(roles, UserRole.APP_PASSWORDS_VIEWER)) {
            appPasswords = safeGet(() -> appPasswordsService.getOverview(adminEmail, true, disabled));
        }

        DnsRecordResponseDto dns = null;
        if (hasAccessToModule(roles, UserRole.DOMAIN_DNS_VIEWER)) {
            dns = getDnsData(adminEmail);
        }

        // DNS
        if (dns != null && dns.rows() != null) {
            Set<String> criticalDnsTypes = new HashSet<>();
            Set<String> attentionDnsTypes = new HashSet<>();
            for (DnsRecordDto row : dns.rows()) {
                if (row.status() == DnsRecordStatus.ACTION_REQUIRED || row.status() == DnsRecordStatus.ERROR) {
                    criticalDnsTypes.add(DNS_TITLES.getOrDefault(row.type(), row.type()));
                } else if (row.status() == DnsRecordStatus.ATTENTION
                        && (row.importance() == DnsRecordImportance.REQUIRED || row.importance() == DnsRecordImportance.RECOMMENDED)) {
                    attentionDnsTypes.add(DNS_TITLES.getOrDefault(row.type(), row.type()));
                }
            }
            if (!criticalDnsTypes.isEmpty()) {
                String typesList = String.join(", ", criticalDnsTypes);
                String messageKey = (criticalDnsTypes.size() == 1) ? "notifications.dns.critical.description.singular" : "notifications.dns.critical.description.plural";
                String description = messageSource.getMessage(messageKey, new Object[]{typesList}, locale);
                notifications.add(
                        create(++id,
                                "critical",
                                messageSource.getMessage("notifications.dns.critical.title", null, locale),
                        description,
                        List.of(messageSource.getMessage("notifications.dns.critical.actions", null, locale)),
                        "dns-critical", "domain-dns", messageSource.getMessage("notifications.dns.label", null, locale), "/domain-dns"));
            }
            if (!attentionDnsTypes.isEmpty()) {
                String typesList = String.join(", ", attentionDnsTypes);
                String messageKey = (attentionDnsTypes.size() == 1) ? "notifications.dns.attention.description.singular" : "notifications.dns.attention.description.plural";
                String description = messageSource.getMessage(messageKey, new Object[]{typesList}, locale);
                notifications.add(create(++id, "warning", messageSource.getMessage("notifications.dns.attention.title", null, locale),
                        description,
                        List.of(messageSource.getMessage("notifications.dns.attention.actions", null, locale)),
                        "dns-attention", "domain-dns", messageSource.getMessage("notifications.dns.label", null, locale), "/domain-dns"));
            }
        }

        // Users
        if (users != null) {
            if (users.withoutTwoFactor() > 0) {
                notifications.add(create(++id, "critical", messageSource.getMessage("notifications.users.without_2fa.title", null, locale),
                        messageSource.getMessage("notifications.users.without_2fa.description", new Object[]{users.withoutTwoFactor()}, locale),
                        List.of(
                                messageSource.getMessage("notifications.users.without_2fa.actions.1", null, locale),
                                messageSource.getMessage("notifications.users.without_2fa.actions.2", null, locale)
                        ),
                        "user-control", "users-groups", messageSource.getMessage("notifications.users_groups.label", null, locale), "/users-groups"));
            }
            if (users.activeLongNoLoginCount() > 0) {
                notifications.add(create(++id, "warning", messageSource.getMessage("notifications.users.active_no_login.title", null, locale),
                        messageSource.getMessage("notifications.users.active_no_login.description", new Object[]{users.activeLongNoLoginCount()}, locale),
                        List.of(messageSource.getMessage("notifications.users.active_no_login.actions", null, locale)),
                        "user-activity", "users-groups", messageSource.getMessage("notifications.users_groups.label", null, locale), "/users-groups"));
            }
            if (users.inactiveRecentLoginCount() > 0) {
                notifications.add(create(++id, "warning", messageSource.getMessage("notifications.users.inactive_recent.title", null, locale),
                        messageSource.getMessage("notifications.users.inactive_recent.description", new Object[]{users.inactiveRecentLoginCount()}, locale),
                        List.of(messageSource.getMessage("notifications.users.inactive_recent.actions", null, locale)),
                        "user-activity", "users-groups", messageSource.getMessage("notifications.users_groups.label", null, locale), "/users-groups"));
            }
        }

        // Groups
        if (groups != null && groups.groupsWithExternal() > 0) {
            notifications.add(create(++id, "info", messageSource.getMessage("notifications.groups.title", null, locale),
                    messageSource.getMessage("notifications.groups.description", new Object[]{groups.groupsWithExternal()}, locale),
                    List.of(messageSource.getMessage("notifications.groups.actions", null, locale)),
                    "group-external", "users-groups", messageSource.getMessage("notifications.users_groups.label", null, locale), "/users-groups"));
        }

        // Drives
        if (drives != null) {
            if (drives.orphanDrives() > 0) {
                notifications.add(create(++id, "warning", messageSource.getMessage("notifications.drives.orphan.title", null, locale),
                        messageSource.getMessage("notifications.drives.orphan.description", new Object[]{drives.orphanDrives()}, locale),
                        List.of(messageSource.getMessage("notifications.drives.orphan.actions", null, locale)),
                        "drive-orphan", "shared-drives", messageSource.getMessage("notifications.drives.label", null, locale), "/shared-drives"));
            }
            if (drives.externalMembersDriveCount() > 0) {
                notifications.add(create(++id, "info", messageSource.getMessage("notifications.drives.external.title", null, locale),
                        messageSource.getMessage("notifications.drives.external.description", new Object[]{drives.externalMembersDriveCount()}, locale),
                        List.of(messageSource.getMessage("notifications.drives.external.actions", null, locale)),
                        "drive-external", "shared-drives", messageSource.getMessage("notifications.drives.label", null, locale), "/shared-drives"));
            }
            if (drives.notOnlyDomainUsersAllowedCount() > 0) {
                notifications.add(create(++id, "warning",
                        messageSource.getMessage("notifications.drives.outside_domain.title", null, locale),
                        messageSource.getMessage("notifications.drives.outside_domain.description", new Object[]{drives.notOnlyDomainUsersAllowedCount()}, locale),
                        List.of(messageSource.getMessage("notifications.drives.outside_domain.actions", null, locale)),
                        "drive-outside-domain", "shared-drives", messageSource.getMessage("notifications.drives.label", null, locale), "/shared-drives"));
            }
            if (drives.notOnlyMembersCanAccessCount() > 0) {
                notifications.add(create(++id, "warning",
                        messageSource.getMessage("notifications.drives.non_member_access.title", null, locale),
                        messageSource.getMessage("notifications.drives.non_member_access.description", new Object[]{drives.notOnlyMembersCanAccessCount()}, locale),
                        List.of(messageSource.getMessage("notifications.drives.non_member_access.actions", null, locale)),
                        "drive-non-member-access", "shared-drives", messageSource.getMessage("notifications.drives.label", null, locale), "/shared-drives"));
            }
        }

        // Devices
        if (devices != null) {
            if (devices.lockScreenCount() > 0) {
                notifications.add(create(++id, "warning", messageSource.getMessage("notifications.devices.lock.title", null, locale),
                        messageSource.getMessage("notifications.devices.lock.description", new Object[]{devices.lockScreenCount()}, locale),
                        List.of( messageSource.getMessage("notifications.devices.lock.actions", null, locale)),
                        "device-lockscreen", "devices", messageSource.getMessage("notifications.devices.label", null, locale), "/devices"));
            }
            if (devices.encryptionCount() > 0) {
                notifications.add(create(++id, "warning", messageSource.getMessage("notifications.devices.enc.title", null, locale),
                        messageSource.getMessage("notifications.devices.enc.description", new Object[]{devices.encryptionCount()}, locale),
                        List.of( messageSource.getMessage("notifications.devices.enc.actions", null, locale)),
                        "device-encryption", "devices", messageSource.getMessage("notifications.devices.label", null, locale), "/devices"));
            }
            if (devices.osVersionCount() > 0) {
                notifications.add(create(++id, "warning", messageSource.getMessage("notifications.devices.os.title", null, locale),
                        messageSource.getMessage("notifications.devices.os.description", new Object[]{devices.osVersionCount()}, locale),
                        List.of( messageSource.getMessage("notifications.devices.os.actions", null, locale)),
                        "device-os", "devices", messageSource.getMessage("notifications.devices.label", null, locale), "/devices"));
            }
            if (devices.integrityCount() > 0) {
                notifications.add(create(++id, "warning", messageSource.getMessage("notifications.devices.int.title", null, locale),
                        messageSource.getMessage("notifications.devices.int.description", new Object[]{devices.integrityCount()}, locale),
                        List.of( messageSource.getMessage("notifications.devices.int.actions", null, locale)),
                        "device-integrity", "devices", messageSource.getMessage("notifications.devices.label", null, locale), "/devices"));
            }
        }

        // OAuth
        if (oAuth != null && oAuth.totalHighRiskApps() > 0) {
            notifications.add(create(++id, "critical", messageSource.getMessage("notifications.app_access.title", null, locale),
                    messageSource.getMessage("notifications.app_access.description", new Object[]{oAuth.totalHighRiskApps()}, locale),
                    List.of(
                            messageSource.getMessage("notifications.app_access.actions.1", null, locale),
                            messageSource.getMessage("notifications.app_access.actions.2", null, locale)
                    ),
                    "oauth-high-risk", "app-access", messageSource.getMessage("notifications.app_access.label", null, locale), "/app-access"));
        }

        // App passwords
        if (appPasswords != null && appPasswords.totalAppPasswords() > 0) {
            notifications.add(create(++id, "info", messageSource.getMessage("notifications.app_passwords.title", null, locale),
                    messageSource.getMessage("notifications.app_passwords.description", new Object[]{appPasswords.totalAppPasswords()}, locale),
                    List.of(messageSource.getMessage("notifications.app_passwords.actions", null, locale)),
                    "app-password", "app-passwords", messageSource.getMessage("notifications.app_passwords.label", null, locale), "/app-passwords"));
        }

        // Password settings
        if (hasAccessToModule(roles, UserRole.PASSWORD_SETTINGS_VIEWER)) {
            PasswordSettingsDto passwordSettings = safeGet(() -> passwordSettingsService.getPasswordSettings(adminEmail));

            if (passwordSettings != null) {
                var twoStep = passwordSettings.twoStepVerification();
                var policies = passwordSettings.passwordPoliciesByOu();
                var adminsWithoutKeys = passwordSettings.adminsWithoutSecurityKeys();

                // Critical: 2SV not enforced in some OUs
                long ousWithout2Sv = twoStep.byOrgUnit().stream().filter(ou -> !ou.enforced()).count();
                if (ousWithout2Sv > 0) {
                    notifications.add(create(++id, "critical",
                            messageSource.getMessage("notifications.password_settings.without_2SV.title", null, locale),
                            messageSource.getMessage("notifications.password_settings.without_2SV.description", new Object[]{ousWithout2Sv}, locale),
                            List.of(messageSource.getMessage("notifications.password_settings.without_2SV.actions", null, locale)),
                            "password-2sv-not-enforced", "password-settings",  messageSource.getMessage("notifications.password_settings.label", null, locale), "/password-settings"));
                }

                // Warning: weak password length (< 12)
                long ousWeakLength = policies.stream()
                        .filter(p -> p.minLength() != null && p.minLength() < 12)
                        .count();
                if (ousWeakLength > 0) {
                    notifications.add(create(++id, "warning", messageSource.getMessage("notifications.password_settings.weak_length.title", null, locale),
                            messageSource.getMessage("notifications.password_settings.weak_length.description", new Object[]{ousWeakLength}, locale),
                            List.of(messageSource.getMessage("notifications.password_settings.weak_length.actions", null, locale)),
                            "password-weak-length", "password-settings", messageSource.getMessage("notifications.password_settings.label", null, locale), "/password-settings"));
                }

                // Warning: strong password not required
                long ousNoStrong = policies.stream()
                        .filter(p -> Boolean.FALSE.equals(p.strongPasswordRequired()))
                        .count();
                if (ousNoStrong > 0) {
                    notifications.add(create(++id, "warning", messageSource.getMessage("notifications.password_settings.no_strong.title", null, locale),
                            messageSource.getMessage("notifications.password_settings.no_strong.description", new Object[]{ousNoStrong}, locale),
                            List.of(messageSource.getMessage("notifications.password_settings.no_strong.actions", null, locale)),
                            "password-strong-not-required", "password-settings", messageSource.getMessage("notifications.password_settings.label", null, locale), "/password-settings"));
                }

                // Warning: password never expires
                long ousNoExpiry = policies.stream()
                        .filter(p -> p.expirationDays() == null || p.expirationDays() == 0)
                        .count();
                if (ousNoExpiry > 0) {
                    notifications.add(create(++id, "warning", messageSource.getMessage("notifications.password_settings.no_expiry.title", null, locale),
                            messageSource.getMessage("notifications.password_settings.no_expiry.description", new Object[]{ousNoExpiry}, locale),
                            List.of(messageSource.getMessage("notifications.password_settings.no_expiry.actions", null, locale)),
                            "password-never-expires", "password-settings", messageSource.getMessage("notifications.password_settings.label", null, locale), "/password-settings"));
                }

                // Warning: admins without security keys
                if (adminsWithoutKeys != null && !adminsWithoutKeys.isEmpty()) {
                    notifications.add(create(++id, "warning", messageSource.getMessage("notifications.password_settings.without_keys.title", null, locale),
                            messageSource.getMessage("notifications.password_settings.without_keys.description", new Object[]{adminsWithoutKeys.size()}, locale),
                            List.of(messageSource.getMessage("notifications.password_settings.without_keys.actions", null, locale)),
                            "password-admins-no-security-keys", "password-settings", messageSource.getMessage("notifications.password_settings.label", null, locale), "/password-settings"));
                }
            }
        }

        // Deduplicate by source-notificationType
        Set<String> seen = new HashSet<>();
        return notifications.stream()
                .filter(n -> seen.add(n.source() + ":" + n.notificationType()))
                .toList();
    }

    private DnsRecordResponseDto getDnsData(String adminEmail) {
        try {
            List<DomainDto> domains = domainService.getAllDomains(adminEmail);
            String primaryDomain = domains.stream()
                    .filter(d -> "Primary Domain".equals(d.domainType()))
                    .findFirst()
                    .map(DomainDto::domainName)
                    .orElse(null);
            if (primaryDomain == null || primaryDomain.isBlank()) {
                return new DnsRecordResponseDto("", List.<DnsRecordDto>of(), 0, null);
            }
            return dnsRecordsService.getImportantRecords(primaryDomain, "google",
                    preferenceService.getDnsImportanceOverrides(adminEmail));
        } catch (Exception e) {
            log.warn("Failed to fetch DNS data for notifications: {}", e.getMessage());
            return new DnsRecordResponseDto("", List.<DnsRecordDto>of(), 0, null);
        }
    }

    private <T> T safeGet(Supplier<T> supplier) {
        try {
            return supplier.get();
        } catch (Exception e) {
            log.warn("Failed to fetch overview data for notifications: {}", e.getMessage());
            return null;
        }
    }

    private NotificationDto create(int id, String severity, String title, String description,
                                  List<String> recommendedActions, String notificationType,
                                  String source, String sourceLabel, String sourceRoute) {
        boolean supportsDetails = NOTIFICATION_TYPES_WITH_DETAILS.contains(notificationType);
        return new NotificationDto(
                "n-" + id,
                severity,
                title,
                description,
                recommendedActions,
                notificationType,
                source,
                sourceLabel,
                sourceRoute,
                false,
                supportsDetails,
                null);
    }

}
