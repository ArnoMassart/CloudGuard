package com.cloudmen.cloudguard.service;

import com.cloudmen.cloudguard.domain.model.User;
import com.cloudmen.cloudguard.domain.model.UserRole;
import com.cloudmen.cloudguard.dto.dashboard.DashboardOverviewResponse;
import com.cloudmen.cloudguard.dto.dashboard.DashboardPageResponse;
import com.cloudmen.cloudguard.dto.dashboard.DashboardScores;
import com.cloudmen.cloudguard.exception.GoogleWorkspaceSyncException;
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
import java.util.concurrent.CompletionException;

import static com.cloudmen.cloudguard.utility.GoogleServiceHelperMethods.hasAccessToModule;

@Service
public class DashboardService {
    private static final Logger log = LoggerFactory.getLogger(DashboardService.class);

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
        try {
            DashboardScores scores = getAllScores(loggedInEmail);
            int overallScore = calculateTotalScore(scores);
            LocalDateTime lastUpdated = LocalDateTime.now();

            return new DashboardPageResponse(
                    scores,
                    overallScore,
                    DateTimeConverter.parseWithPattern(lastUpdated, "d MMMM yyyy, HH:mm")
            );
        } catch (Exception ex) {
            // Pak de originele error (ontmantel de CompletionException van de async taken)
            Throwable cause = (ex instanceof CompletionException) ? ex.getCause() : ex;

            // Als het onze specifieke Google error is, gooi deze 1-op-1 door naar de GlobalExceptionHandler!
            if (cause instanceof GoogleWorkspaceSyncException) {
                throw (GoogleWorkspaceSyncException) cause;
            }

            log.error("Error with getting the dashboard data: {}", cause.getMessage());
            throw new GoogleWorkspaceSyncException("Error with getting the dashboard data: " + cause.getMessage(), cause);
        }
    }

    public DashboardOverviewResponse getDashboardOverview(String loggedInEmail) {
        try {
            int totalNotifications = (int) notificationService.getNotificationsCount(loggedInEmail);
            int criticalNotifications = (int) notificationService.getNotificationsCriticalCount(loggedInEmail);
            return new DashboardOverviewResponse(totalNotifications, criticalNotifications);
        } catch (Exception e) {
            Throwable cause = (e instanceof CompletionException) ? e.getCause() : e;

            // Gooi de error naar de frontend als het de specifieke "No Admin" melding betreft
            if (cause instanceof GoogleWorkspaceSyncException && cause.getMessage() != null && cause.getMessage().contains("No Admin email")) {
                throw (GoogleWorkspaceSyncException) cause;
            }

            log.error("Fout bij ophalen notificaties voor dashboard: {}", cause.getMessage());
            return new DashboardOverviewResponse(0, 0);
        }
    }

    private DashboardScores getAllScores(String loggedInEmail) {
        try {
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
                    () -> passwordsService.getOverview(loggedInEmail ,disabled).securityScore(),
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
                                    ).exceptionally(ex -> handleAsyncError(ex, "Individuele DNS", 0))) // <-- Gebruik helper
                                    .toList();

                            return CompletableFuture.allOf(dnsTasks.toArray(new CompletableFuture[0]))
                                    .thenApply(v -> {
                                        int totalScore = dnsTasks.stream().mapToInt(CompletableFuture::join).sum();
                                        return Math.round((float) totalScore / domains.size());
                                    });
                        }).exceptionally(ex -> handleAsyncError(ex, "Globale DNS Average", 100)); // <-- Gebruik helper
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
        } catch (Exception ex) {
            Throwable cause = (ex instanceof CompletionException) ? ex.getCause() : ex;
            if (cause instanceof GoogleWorkspaceSyncException) {
                throw (GoogleWorkspaceSyncException) cause;
            }
            throw new GoogleWorkspaceSyncException("Error with getting the dashboard scores: " + cause.getMessage(), cause);
        }
    }

    private int calculateTotalScore(DashboardScores scores) {
        int totalScore = 0;
        int categoriesCount = 0;

        if (scores.usersScore() >= 0) { totalScore += scores.usersScore(); categoriesCount++; }
        if (scores.groupsScore() >= 0) { totalScore += scores.groupsScore(); categoriesCount++; }
        if (scores.drivesScore() >= 0) { totalScore += scores.drivesScore(); categoriesCount++; }
        if (scores.devicesScore() >= 0) { totalScore += scores.devicesScore(); categoriesCount++; }
        if (scores.appAccessScore() >= 0) { totalScore += scores.appAccessScore(); categoriesCount++; }
        if (scores.appPasswordsScore() >= 0) { totalScore += scores.appPasswordsScore(); categoriesCount++; }
        if (scores.passwordSettingsScore() >= 0) { totalScore += scores.passwordSettingsScore(); categoriesCount++; }
        if (scores.dnsScore() >= 0) { totalScore += scores.dnsScore(); categoriesCount++; }

        if (categoriesCount == 0) {
            return 0;
        }

        return Math.round((float) totalScore / categoriesCount);
    }

    private CompletableFuture<Integer> fetchScoreIfAllowed(List<UserRole> roles, UserRole requiredRole, java.util.function.Supplier<Integer> scoreSupplier, String moduleName) {
        if (hasAccessToModule(roles, requiredRole)) {
            return CompletableFuture.supplyAsync(scoreSupplier)
                    .exceptionally(ex -> handleAsyncError(ex, moduleName, 0)); // <-- Gebruik helper
        }
        return CompletableFuture.completedFuture(-1);
    }

    /**
     * Hulpmethode om asynchrone errors af te handelen.
     * Slikt normale fouten in (geeft fallbackScore terug), maar gooit "No Admin" fouten wél op!
     */
    private int handleAsyncError(Throwable ex, String moduleName, int fallbackScore) {
        Throwable root = ex.getCause() != null ? ex.getCause() : ex;

        // Als het de specifieke admin-fout is, gooi hem dan opnieuw op zodat de join() hem afvangt
        if (root instanceof GoogleWorkspaceSyncException && root.getMessage() != null && root.getMessage().contains("No Admin email")) {
            throw new CompletionException(root);
        }

        log.error("Fout bij laden {} score/logica: {}", moduleName, root.getMessage());
        return fallbackScore;
    }
}