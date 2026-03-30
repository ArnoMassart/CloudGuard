package com.cloudmen.cloudguard.service;

import com.cloudmen.cloudguard.dto.dashboard.DashboardOverviewResponse;
import com.cloudmen.cloudguard.dto.dashboard.DashboardPageResponse;
import com.cloudmen.cloudguard.dto.dashboard.DashboardScores;
import com.cloudmen.cloudguard.service.cache.GoogleUsersCacheService;
import com.cloudmen.cloudguard.service.dns.DnsRecordsService;
import com.cloudmen.cloudguard.service.notification.NotificationAggregationService;
import com.cloudmen.cloudguard.service.preference.UserSecurityPreferenceService;
import com.cloudmen.cloudguard.utility.DateTimeConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class DashboardService {
    private static final Logger log = LoggerFactory.getLogger(DashboardService.class);
    private static final boolean IS_TESTMODE = true;

    private final GoogleUsersService usersService;
    private final GoogleGroupsService groupsService;
    private final GoogleSharedDriveService sharedDriveService;
    private final GoogleDeviceService googleDeviceService;
    private final GoogleOAuthService oAuthService;
    private final AppPasswordsService passwordsService;
    private final DnsRecordsService dnsRecordsService;
    private final GoogleDomainService domainService;
    private final NotificationAggregationService notificationService;
    private final PasswordSettingsService passwordSettingsService;
    private final UserSecurityPreferenceService userSecurityPreferenceService;

    public DashboardService(GoogleUsersService usersService, GoogleGroupsService groupsService, GoogleSharedDriveService sharedDriveService, GoogleDeviceService googleDeviceService, GoogleOAuthService oAuthService, AppPasswordsService passwordsService, DnsRecordsService dnsRecordsService, GoogleDomainService domainService, NotificationAggregationService notificationService, PasswordSettingsService passwordSettingsService, UserSecurityPreferenceService userSecurityPreferenceService) {
        this.usersService = usersService;
        this.groupsService = groupsService;
        this.sharedDriveService = sharedDriveService;
        this.googleDeviceService = googleDeviceService;
        this.oAuthService = oAuthService;
        this.passwordsService = passwordsService;
        this.dnsRecordsService = dnsRecordsService;
        this.domainService = domainService;
        this.notificationService = notificationService;
        this.passwordSettingsService = passwordSettingsService;
        this.userSecurityPreferenceService = userSecurityPreferenceService;
    }

    public DashboardPageResponse getDashboardSecurityScores(String loggedInEmail) {
        DashboardScores scores = getAllScores(loggedInEmail);

        int overallScore = calculateTotalScore(scores);

        LocalDateTime lastUpdated = LocalDateTime.now();

        return new DashboardPageResponse(
                scores,
                overallScore,
                DateTimeConverter.parseWithPattern(lastUpdated, "d MMMM yyyy, HH:mm")
        );
    }

    public DashboardOverviewResponse getDashboardOverview(String loggedInEmail) {
        int totalNotifications = 0;
        int criticalNotifications = 0;

        // Vang fouten hier af zodat de notificatie-widget het dashboard niet breekt
        try {
            totalNotifications = (int) notificationService.getNotificationsCount(loggedInEmail);
            criticalNotifications = (int) notificationService.getNotificationsCriticalCount(loggedInEmail);
        } catch (Exception e) {
            log.error("Fout bij ophalen notificaties voor dashboard: {}", e.getMessage());
        }

        return new DashboardOverviewResponse(totalNotifications, criticalNotifications);
    }

    private DashboardScores getAllScores(String loggedInEmail) {
        var disabled = userSecurityPreferenceService.getDisabledPreferenceKeys(loggedInEmail);

        CompletableFuture<Integer> usersFuture = CompletableFuture.supplyAsync(() ->
                        usersService.getUsersPageOverview(loggedInEmail, disabled).securityScore())
                .exceptionally(ex -> {
                    log.error("Fout bij laden Users score: {}", ex.getMessage());
                    return 100; // Fallback score
                });

        CompletableFuture<Integer> groupsFuture = CompletableFuture.supplyAsync(() ->
                        groupsService.getGroupsOverview(loggedInEmail, disabled).securityScore())
                .exceptionally(ex -> {
                    log.error("Fout bij laden Groups score: {}", ex.getMessage());
                    return 100;
                });

        CompletableFuture<Integer> drivesFuture = CompletableFuture.supplyAsync(() ->
                        sharedDriveService.getDrivesPageOverview(loggedInEmail, disabled).securityScore())
                .exceptionally(ex -> {
                    log.error("Fout bij laden Drives score: {}", ex.getMessage());
                    return 100;
                });

        CompletableFuture<Integer> devicesFuture = CompletableFuture.supplyAsync(() ->
                        googleDeviceService.getDevicesPageOverview(loggedInEmail, disabled).securityScore())
                .exceptionally(ex -> {
                    log.error("Fout bij laden Devices score: {}", ex.getMessage());
                    return 100;
                });

        CompletableFuture<Integer> appAccessFuture = CompletableFuture.supplyAsync(() ->
                        oAuthService.getOAuthPageOverview(loggedInEmail, disabled).securityScore())
                .exceptionally(ex -> {
                    log.error("Fout bij laden App Access score: {}", ex.getMessage());
                    return 100;
                });

        CompletableFuture<Integer> appPasswordsFuture = CompletableFuture.supplyAsync(() ->
                        passwordsService.getOverview(loggedInEmail, IS_TESTMODE, disabled).securityScore())
                .exceptionally(ex -> {
                    log.error("Fout bij laden App Passwords score: {}", ex.getMessage());
                    return 100;
                });

        CompletableFuture<Integer> passwordSettingsFuture = CompletableFuture.supplyAsync(() ->
                        passwordSettingsService.getPasswordSettings(loggedInEmail).securityScore())
                .exceptionally(ex -> {
                    log.error("Fout bij laden Password Settings score: {}", ex.getMessage());
                    return 100;
                });

        CompletableFuture<Integer> dnsAverageFuture = CompletableFuture.supplyAsync(() ->
                        domainService.getAllDomains(loggedInEmail))
                .thenCompose(domains -> {
                    if (domains == null || domains.isEmpty()) {
                        return CompletableFuture.completedFuture(0);
                    }

                    var dnsOverrides = userSecurityPreferenceService.getDnsImportanceOverrides(loggedInEmail);
                    List<CompletableFuture<Integer>> dnsTasks = domains.stream()
                            .map(dto -> CompletableFuture.supplyAsync(() ->
                                    dnsRecordsService.getImportantRecords(dto.domainName(), "google", dnsOverrides).securityScore()
                            ).exceptionally(ex -> {
                                log.error("Fout bij berekenen DNS score voor domein {}: {}", dto.domainName(), ex.getMessage());
                                return 100; // Vangt fouten per specifiek domein af
                            }))
                            .toList();

                    return CompletableFuture.allOf(dnsTasks.toArray(new CompletableFuture[0]))
                            .thenApply(v -> {
                                int totalScore = dnsTasks.stream()
                                        .mapToInt(CompletableFuture::join)
                                        .sum();

                                return Math.round((float) totalScore / domains.size());
                            });
                })
                .exceptionally(ex -> {
                    log.error("Fout in globale DNS Average logica: {}", ex.getMessage());
                    return 100;
                });

        // Wacht tot alle taken klaar zijn (lukken ze niet? Dan geven ze door the .exceptionally gewoon 0 terug!)
        CompletableFuture.allOf(
                usersFuture, groupsFuture, drivesFuture, devicesFuture, appAccessFuture, appPasswordsFuture,
                passwordSettingsFuture, dnsAverageFuture
        ).join();

        int usersScore = usersFuture.join();
        int groupsScore = groupsFuture.join();
        int drivesScore = drivesFuture.join();
        int devicesScore = devicesFuture.join();
        int appAccessScore = appAccessFuture.join();
        int appPasswordsScore = appPasswordsFuture.join();
        int passwordSettingsScore = passwordSettingsFuture.join();
        int dnsScore = dnsAverageFuture.join();

        return new DashboardScores(usersScore, groupsScore, drivesScore,
                devicesScore, appAccessScore, appPasswordsScore, passwordSettingsScore, dnsScore);
    }

    private int calculateTotalScore(DashboardScores scores) {
        int totalCategories = 8;
        int totalScoresAdded = scores.usersScore() + scores.groupsScore() + scores.drivesScore() + scores.devicesScore() + scores.appAccessScore() + scores.appPasswordsScore()+scores.passwordSettingsScore()+scores.dnsScore();


        return Math.round((float)totalScoresAdded / totalCategories);
    }
}

