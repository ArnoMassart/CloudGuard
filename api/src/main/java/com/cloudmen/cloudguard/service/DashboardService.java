package com.cloudmen.cloudguard.service;

import com.cloudmen.cloudguard.domain.model.User;
import com.cloudmen.cloudguard.domain.model.UserRole;
import com.cloudmen.cloudguard.dto.dashboard.DashboardOverviewResponse;
import com.cloudmen.cloudguard.dto.dashboard.DashboardPageResponse;
import com.cloudmen.cloudguard.dto.dashboard.DashboardScores;
import com.cloudmen.cloudguard.service.dns.DnsRecordsService;
import com.cloudmen.cloudguard.service.notification.NotificationAggregationService;
import com.cloudmen.cloudguard.service.preference.UserSecurityPreferenceService;
import com.cloudmen.cloudguard.utility.DateTimeConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static com.cloudmen.cloudguard.utility.GoogleServiceHelperMethods.hasAccessToModule;

@Service
public class DashboardService {
    private static final Logger log = LoggerFactory.getLogger(DashboardService.class);
    private static final boolean IS_TESTMODE = false;

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
    private final UserService userService;

    public DashboardService(GoogleUsersService usersService, GoogleGroupsService groupsService, GoogleSharedDriveService sharedDriveService, GoogleDeviceService googleDeviceService, GoogleOAuthService oAuthService, AppPasswordsService passwordsService, DnsRecordsService dnsRecordsService, GoogleDomainService domainService, NotificationAggregationService notificationService, PasswordSettingsService passwordSettingsService, UserSecurityPreferenceService userSecurityPreferenceService, UserService userService) {
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
        this.userService = userService;
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
        User user = userService.findByEmail(loggedInEmail);
        List<UserRole> roles = user.getRoles();

        var disabled = userSecurityPreferenceService.getDisabledPreferenceKeys(loggedInEmail);

        CompletableFuture<Integer> usersFuture = fetchScoreIfAllowed(
                roles,
                UserRole.USERS_GROUPS_VIEWER,
                () -> usersService.getUsersPageOverview(loggedInEmail, disabled).securityScore(),
                "Users"
        );

        CompletableFuture<Integer> groupsFuture = fetchScoreIfAllowed(
                roles,
                UserRole.USERS_GROUPS_VIEWER,
                () -> groupsService.getGroupsOverview(loggedInEmail, disabled).securityScore(),
                "Groups"
        );

        CompletableFuture<Integer> drivesFuture = fetchScoreIfAllowed(
                roles,
                UserRole.SHARED_DRIVES_VIEWER,
                () -> sharedDriveService.getDrivesPageOverview(loggedInEmail, disabled).securityScore(),
                "Drives"
                );

        CompletableFuture<Integer> devicesFuture = fetchScoreIfAllowed(
                roles,
                UserRole.DEVICES_VIEWER,
                () -> googleDeviceService.getDevicesPageOverview(loggedInEmail, disabled).securityScore(),
                "Devices"
        );

        CompletableFuture<Integer> appAccessFuture = fetchScoreIfAllowed(
                roles,
                UserRole.APP_ACCESS_VIEWER,
                () -> oAuthService.getOAuthPageOverview(loggedInEmail, disabled).securityScore(),
                "App Access"
        );

        CompletableFuture<Integer> appPasswordsFuture = fetchScoreIfAllowed(
                roles,
                UserRole.APP_PASSWORDS_VIEWER,
                () -> passwordsService.getOverview(loggedInEmail, IS_TESTMODE ,disabled).securityScore(),
                "App Passwords"
                );

        CompletableFuture<Integer> passwordSettingsFuture = fetchScoreIfAllowed(
                roles,
                UserRole.PASSWORD_SETTINGS_VIEWER,
                () -> passwordSettingsService.getPasswordSettings(loggedInEmail).securityScore(),
                "Password Settings"
        );

        CompletableFuture<Integer> dnsAverageFuture;
        if (hasAccessToModule(roles, UserRole.DOMAIN_DNS_VIEWER)) {
            dnsAverageFuture = CompletableFuture.supplyAsync(() -> domainService.getAllDomains(loggedInEmail))
                    .thenCompose(domains -> {
                        if (domains == null || domains.isEmpty()) return CompletableFuture.completedFuture(0);

                        var dnsOverrides = userSecurityPreferenceService.getDnsImportanceOverrides(loggedInEmail);
                        List<CompletableFuture<Integer>> dnsTasks = domains.stream()
                                .map(dto -> CompletableFuture.supplyAsync(() ->
                                        dnsRecordsService.getImportantRecords(dto.domainName(), "google", dnsOverrides).securityScore()
                                ).exceptionally(ex -> 100))
                                .toList();

                        return CompletableFuture.allOf(dnsTasks.toArray(new CompletableFuture[0]))
                                .thenApply(v -> {
                                    int totalScore = dnsTasks.stream().mapToInt(CompletableFuture::join).sum();
                                    return Math.round((float) totalScore / domains.size());
                                });
                    }).exceptionally(ex -> {
                        log.error("Fout in globale DNS Average logica: {}", ex.getMessage());
                        return 100;
                    });
        } else {
            dnsAverageFuture = CompletableFuture.completedFuture(-1);
        }

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
        int totalScore = 0;
        int categoriesCount = 0;

        // Tel score alleen mee als deze niet -1 is
        if (scores.usersScore() >= 0) { totalScore += scores.usersScore(); categoriesCount++; }
        if (scores.groupsScore() >= 0) { totalScore += scores.groupsScore(); categoriesCount++; }
        if (scores.drivesScore() >= 0) { totalScore += scores.drivesScore(); categoriesCount++; }
        if (scores.devicesScore() >= 0) { totalScore += scores.devicesScore(); categoriesCount++; }
        if (scores.appAccessScore() >= 0) { totalScore += scores.appAccessScore(); categoriesCount++; }
        if (scores.appPasswordsScore() >= 0) { totalScore += scores.appPasswordsScore(); categoriesCount++; }
        if (scores.passwordSettingsScore() >= 0) { totalScore += scores.passwordSettingsScore(); categoriesCount++; }
        if (scores.dnsScore() >= 0) { totalScore += scores.dnsScore(); categoriesCount++; }

        // Voorkom 'divide by zero' als de gebruiker nergens rechten voor heeft
        if (categoriesCount == 0) {
            return 0;
        }

        return Math.round((float) totalScore / categoriesCount);
    }

    private CompletableFuture<Integer> fetchScoreIfAllowed(List<UserRole> roles, UserRole requiredRole, java.util.function.Supplier<Integer> scoreSupplier, String moduleName) {
        if (hasAccessToModule(roles, requiredRole)) {
            return CompletableFuture.supplyAsync(scoreSupplier)
                    .exceptionally(ex -> {
                        log.error("Fout bij laden {} score: {}", moduleName, ex.getMessage());
                        return 100; // Fallback score bij error
                    });
        }

        return CompletableFuture.completedFuture(-1);
    }
}

