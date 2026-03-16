package com.cloudmen.cloudguard.service.notification;

import com.cloudmen.cloudguard.domain.model.feedback.ResolvedNotification;
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
import com.cloudmen.cloudguard.dto.users.UserOverviewResponse;
import com.cloudmen.cloudguard.service.*;
import com.cloudmen.cloudguard.service.dns.DnsRecordsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

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
            "device-lockscreen", "device-encryption", "device-os", "device-integrity"
    );

    private final GoogleDomainService domainService;
    private final DnsRecordsService dnsRecordsService;
    private final GoogleUsersService usersService;
    private final GoogleSharedDriveService driveService;
    private final GoogleDeviceService deviceService;
    private final AppPasswordsService appPasswordsService;
    private final GoogleGroupsService groupsService;
    private final GoogleOAuthService oAuthService;
    private final ResolvedNotificationService resolvedService;
    private final NotificationFeedbackService feedbackService;

    public NotificationAggregationService(
            GoogleDomainService domainService,
            DnsRecordsService dnsRecordsService,
            GoogleUsersService usersService,
            GoogleSharedDriveService driveService,
            GoogleDeviceService deviceService,
            AppPasswordsService appPasswordsService,
            GoogleGroupsService groupsService,
            GoogleOAuthService oAuthService,
            ResolvedNotificationService resolvedService,
            NotificationFeedbackService feedbackService) {
        this.domainService = domainService;
        this.dnsRecordsService = dnsRecordsService;
        this.usersService = usersService;
        this.driveService = driveService;
        this.deviceService = deviceService;
        this.appPasswordsService = appPasswordsService;
        this.groupsService = groupsService;
        this.oAuthService = oAuthService;
        this.resolvedService = resolvedService;
        this.feedbackService = feedbackService;
    }

    public NotificationsResponse getNotifications(String userId) {
        List<NotificationDto> active = aggregateActive(userId);
        List<ResolvedNotification> resolved = resolvedService.getResolvedForUser(userId);
        Set<String> resolvedKeys = resolved.stream()
                .map(r -> r.getSource() + ":" + r.getNotificationType())
                .collect(Collectors.toSet());
        Set<String> feedbackKeys = feedbackService.getFeedbackKeysForUser(userId);

        List<NotificationDto> filtered = active.stream()
                .filter(n -> !resolvedKeys.contains(n.source() + ":" + n.notificationType()))
                .map(n -> withStatus(n, feedbackKeys))
                .toList();

        List<NotificationDto> resolvedDtos = resolved.stream()
                .map(this::toResolvedDto)
                .toList();

        return new NotificationsResponse(filtered, resolvedDtos);
    }

    public long getNotificationsCount(String userId) {
        NotificationsResponse response = getNotifications(userId);
        return response.active().size();
    }

    public long getNotificationsCriticalCount(String userId) {
        NotificationsResponse response = getNotifications(userId);
        return response.active().stream()
                .filter(n -> "critical".equals(n.severity()))
                .count();
    }

    private NotificationDto withStatus(NotificationDto n, Set<String> feedbackKeys) {
        String status = feedbackKeys.contains(n.source() + ":" + n.notificationType()) ? "in_behandeling" : "new";
        return new NotificationDto(n.id(), n.severity(), n.title(), n.description(), n.recommendedActions(),
                n.notificationType(), n.source(), n.sourceLabel(), n.sourceRoute(), status, n.supportsDetails());
    }

    private NotificationDto toResolvedDto(ResolvedNotification r) {
        boolean supportsDetails = NOTIFICATION_TYPES_WITH_DETAILS.contains(r.getNotificationType());
        return new NotificationDto(
                r.getId().toString(),
                r.getSeverity(),
                r.getTitle(),
                r.getDescription(),
                r.getRecommendedActions() != null ? r.getRecommendedActions() : List.of(),
                r.getNotificationType(),
                r.getSource(),
                r.getSourceLabel(),
                r.getSourceRoute(),
                "resolved",
                supportsDetails
        );
    }

    private List<NotificationDto> aggregateActive(String adminEmail) {
        List<NotificationDto> notifications = new ArrayList<>();
        int id = 0;

        UserOverviewResponse users = safeGet(() -> usersService.getUsersPageOverview(adminEmail));
        SharedDriveOverviewResponse drives = safeGet(() -> driveService.getDrivesPageOverview(adminEmail));
        DeviceOverviewResponse devices = safeGet(() -> deviceService.getDevicesPageOverview(adminEmail));
        GroupOverviewResponse groups = safeGet(() -> groupsService.getGroupsOverview(adminEmail));
        OAuthOverviewResponse oAuth = safeGet(() -> oAuthService.getOAuthPageOverview(adminEmail));
        AppPasswordOverviewResponse appPasswords = safeGet(() -> appPasswordsService.getOverview(adminEmail, true));

        DnsRecordResponseDto dns = getDnsData(adminEmail);

        // DNS
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
            notifications.add(create(++id, "critical", "DNS records ontbreken of niet correct",
                    typesList + (criticalDnsTypes.size() == 1 ? " ontbreekt" : " ontbreken") + " of " + (criticalDnsTypes.size() == 1 ? "is niet" : "zijn niet") + " correct geconfigureerd.",
                    List.of("Controleer en configureer alle DNS records via je DNS provider"),
                    "dns-critical", "domain-dns", "Domein & DNS", "/domain-dns"));
        }
        if (!attentionDnsTypes.isEmpty()) {
            String typesList = String.join(", ", attentionDnsTypes);
            notifications.add(create(++id, "warning", "DNS records vereisen aandacht",
                    typesList + (attentionDnsTypes.size() == 1 ? " kan" : " kunnen") + " worden verbeterd.",
                    List.of("Controleer de DNS configuratie via je DNS provider"),
                    "dns-attention", "domain-dns", "Domein & DNS", "/domain-dns"));
        }

        // Users
        if (users != null) {
            if (users.withoutTwoFactor() > 0) {
                notifications.add(create(++id, "critical", "Gebruikers zonder tweestapsverificatie",
                        users.withoutTwoFactor() + " gebruiker(s) hebben geen 2FA ingeschakeld.",
                        List.of("Schakel 2FA in voor deze gebruikers", "Verwijder admin rechten totdat 2FA is ingeschakeld"),
                        "user-control", "users-groups", "Gebruikers & Groepen", "/users-groups"));
            }
            if (users.activeLongNoLoginCount() > 0) {
                notifications.add(create(++id, "warning", "Actieve gebruikers met lange inactiviteit",
                        users.activeLongNoLoginCount() + " actieve gebruiker(s) hebben lang niet ingelogd.",
                        List.of("Controleer of deze accounts nog actief moeten zijn"),
                        "user-activity", "users-groups", "Gebruikers & Groepen", "/users-groups"));
            }
            if (users.inactiveRecentLoginCount() > 0) {
                notifications.add(create(++id, "warning", "Inactieve gebruikers met recente login",
                        users.inactiveRecentLoginCount() + " inactieve gebruiker(s) hebben recent ingelogd.",
                        List.of("Controleer of deze accounts opnieuw geactiveerd moeten worden"),
                        "user-activity", "users-groups", "Gebruikers & Groepen", "/users-groups"));
            }
        }

        // Groups
        if (groups != null && groups.groupsWithExternal() > 0) {
            notifications.add(create(++id, "info", "Groepen met externe leden",
                    groups.groupsWithExternal() + " groep(en) hebben externe leden.",
                    List.of("Controleer toegangsrechten van externe leden"),
                    "group-external", "users-groups", "Gebruikers & Groepen", "/users-groups"));
        }

        // Drives
        if (drives != null) {
            if (drives.orphanDrives() > 0) {
                notifications.add(create(++id, "warning", "Gedeelde drives zonder eigenaar",
                        drives.orphanDrives() + " drive(s) hebben geen actieve eigenaar.",
                        List.of("Wijs een nieuwe eigenaar toe aan deze drives"),
                        "drive-orphan", "shared-drives", "Gedeelde Drives", "/shared-drives"));
            }
            if (drives.externalMembersDriveCount() > 0) {
                notifications.add(create(++id, "info", "Drives met externe leden",
                        drives.externalMembersDriveCount() + " drive(s) hebben externe leden.",
                        List.of("Controleer externe toegang tot gedeelde drives"),
                        "drive-external", "shared-drives", "Gedeelde Drives", "/shared-drives"));
            }
        }

        // Devices
        if (devices != null) {
            if (devices.lockScreenCount() > 0) {
                notifications.add(create(++id, "warning", "Apparaten zonder vergrendelscherm beveiliging",
                        devices.lockScreenCount() + " apparaat(en) hebben geen vergrendelscherm beveiliging.",
                        List.of("Vereis lockscreen voor alle apparaten"),
                        "device-lockscreen", "mobile-devices", "Mobiele Apparaten", "/mobile-devices"));
            }
            if (devices.encryptionCount() > 0) {
                notifications.add(create(++id, "warning", "Apparaten zonder encryptie",
                        devices.encryptionCount() + " apparaat(en) zijn niet versleuteld.",
                        List.of("Schakel apparaatencryptie in"),
                        "device-encryption", "mobile-devices", "Mobiele Apparaten", "/mobile-devices"));
            }
            if (devices.osVersionCount() > 0) {
                notifications.add(create(++id, "warning", "Apparaten met verouderde OS versie",
                        devices.osVersionCount() + " apparaat(en) hebben een verouderde besturingssysteemversie.",
                        List.of("Vereis minimale OS-versie voor alle apparaten"),
                        "device-os", "mobile-devices", "Mobiele Apparaten", "/mobile-devices"));
            }
            if (devices.integrityCount() > 0) {
                notifications.add(create(++id, "warning", "Apparaten met integriteitsproblemen",
                        devices.integrityCount() + " apparaat(en) hebben integriteitsproblemen (root/jailbreak).",
                        List.of("Blokkeer geroote of gejailbroken apparaten"),
                        "device-integrity", "mobile-devices", "Mobiele Apparaten", "/mobile-devices"));
            }
        }

        // OAuth
        if (oAuth != null && oAuth.totalHighRiskApps() > 0) {
            notifications.add(create(++id, "critical", "Third-party applicaties met te veel toegang",
                    oAuth.totalHighRiskApps() + " applicatie(s) hebben toegang tot gevoelige gegevens of admin-functies.",
                    List.of("Controleer de toegangsrechten voor deze applicaties", "Herroep toegang voor apps die niet meer nodig zijn"),
                    "oauth-high-risk", "app-access", "App Toegang", "/app-access"));
        }

        // App passwords
        if (appPasswords != null && appPasswords.totalAppPasswords() > 0) {
            notifications.add(create(++id, "info", "App-wachtwoorden actief",
                    appPasswords.totalAppPasswords() + " app-wachtwoord(en) actief. App-wachtwoorden omzeilen 2FA.",
                    List.of("Overweeg OAuth-gebaseerde authenticatie waar mogelijk"),
                    "app-password", "app-passwords", "App-wachtwoorden", "/app-passwords"));
        }

        // Deduplicate by source-notificationType
        Set<String> seen = new HashSet<>();
        return notifications.stream()
                .filter(n -> {
                    String key = n.source() + "-" + n.notificationType();
                    if (seen.contains(key)) return false;
                    seen.add(key);
                    return true;
                })
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
                return new DnsRecordResponseDto("", List.<DnsRecordDto>of(), 0);
            }
            return dnsRecordsService.getImportantRecords(primaryDomain, "google");
        } catch (Exception e) {
            log.warn("Failed to fetch DNS data for notifications: {}", e.getMessage());
            return new DnsRecordResponseDto("", List.<DnsRecordDto>of(), 0);
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
        return new NotificationDto("n-" + id, severity, title, description, recommendedActions,
                notificationType, source, sourceLabel, sourceRoute, null, supportsDetails);
    }

}
