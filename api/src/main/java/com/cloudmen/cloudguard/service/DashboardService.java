package com.cloudmen.cloudguard.service;

import com.cloudmen.cloudguard.dto.dashboard.DashboardOverviewResponse;
import com.cloudmen.cloudguard.dto.dashboard.DashboardPageResponse;
import com.cloudmen.cloudguard.dto.dashboard.DashboardScores;
import com.cloudmen.cloudguard.service.dns.DnsRecordsService;
import com.cloudmen.cloudguard.utility.DateTimeConverter;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
public class DashboardService {
    private final boolean IsTestmode = true;

    private final GoogleUsersService usersService;
    private final GoogleGroupsService groupsService;
    private final GoogleSharedDriveService sharedDriveService;
    private final GoogleMobileDeviceService mobileDeviceService;
    private final GoogleOAuthService oAuthService;
    private final AppPasswordsService passwordsService;
    private final DnsRecordsService dnsRecordsService;
    private final GoogleDomainService domainService;
    private final NotificationService notificationService;

    public DashboardService(GoogleUsersService usersService, GoogleGroupsService groupsService, GoogleSharedDriveService sharedDriveService, GoogleMobileDeviceService mobileDeviceService, GoogleOAuthService oAuthService, AppPasswordsService passwordsService, DnsRecordsService dnsRecordsService, GoogleDomainService domainService, NotificationService notificationService) {
        this.usersService = usersService;
        this.groupsService = groupsService;
        this.sharedDriveService = sharedDriveService;
        this.mobileDeviceService = mobileDeviceService;
        this.oAuthService = oAuthService;
        this.passwordsService = passwordsService;
        this.dnsRecordsService = dnsRecordsService;
        this.domainService = domainService;
        this.notificationService = notificationService;
    }

    public DashboardPageResponse getDashboardSecurityScores(String loggedInEmail) {
        DashboardScores scores = getAllScores(loggedInEmail);

        int overallScore = calculateTotalScore(scores);

        LocalDateTime lastUpdated = LocalDateTime.now();

        return new DashboardPageResponse(
                scores,
                overallScore,
                DateTimeConverter.parseToPattern(lastUpdated, "d MMMM yyyy, HH:mm")
        );
    }

    public DashboardOverviewResponse getDashboardOverview() {
        int totalNotifications = (int) notificationService.getNotificationsCount();
        int criticalNotifications = (int) notificationService.getNotificationsCriticalCount();

        return new DashboardOverviewResponse(totalNotifications, criticalNotifications);
    }

    private DashboardScores getAllScores(String loggedInEmail) {
        // 1. Start de 6 standaard modules tegelijkertijd op (Net als voorheen)
        CompletableFuture<Integer> usersFuture = CompletableFuture.supplyAsync(() ->
                usersService.getUsersPageOverview(loggedInEmail).securityScore());
        CompletableFuture<Integer> groupsFuture = CompletableFuture.supplyAsync(() ->
                groupsService.getGroupsOverview(loggedInEmail).securityScore());
        CompletableFuture<Integer> drivesFuture = CompletableFuture.supplyAsync(() ->
                sharedDriveService.getDrivesPageOverview(loggedInEmail).securityScore());
        CompletableFuture<Integer> devicesFuture = CompletableFuture.supplyAsync(() ->
                mobileDeviceService.getMobileDevicesPageOverview(loggedInEmail, IsTestmode).securityScore());
        CompletableFuture<Integer> appAccessFuture = CompletableFuture.supplyAsync(() ->
                oAuthService.getOAuthPageOverview(loggedInEmail).securityScore());
        CompletableFuture<Integer> appPasswordsFuture = CompletableFuture.supplyAsync(() ->
                passwordsService.getOverview(loggedInEmail, IsTestmode).securityScore());

        // 2. De DNS Berekening: Haal asynchroon de domeinen op, en vuur daarna per domein een check af
        CompletableFuture<Integer> dnsAverageFuture = CompletableFuture.supplyAsync(() ->
                        domainService.getAllDomains(loggedInEmail))
                .thenCompose(domains -> {
                    if (domains == null || domains.isEmpty()) {
                        return CompletableFuture.completedFuture(0);
                    }

                    // Maak voor ELK domein een nieuwe, parallelle DNS-check aan
                    List<CompletableFuture<Integer>> dnsTasks = domains.stream()
                            .map(dto -> CompletableFuture.supplyAsync(() ->
                                    dnsRecordsService.getImportantRecords(dto.domainName(), "google").securityScore()
                            ))
                            .toList();

                    // Wacht tot álle individuele DNS checks van alle domeinen klaar zijn
                    return CompletableFuture.allOf(dnsTasks.toArray(new CompletableFuture[0]))
                            .thenApply(v -> {
                                // Tel alle voltooide scores bij elkaar op
                                int totalScore = dnsTasks.stream()
                                        .mapToInt(CompletableFuture::join)
                                        .sum();

                                // Bereken het gemiddelde. (Geen * 100 meer nodig, want de scores zijn al 0-100)
                                return Math.round((float) totalScore / domains.size());
                            });
                });

        CompletableFuture.allOf(
                usersFuture, groupsFuture, drivesFuture, devicesFuture, appAccessFuture, appPasswordsFuture, dnsAverageFuture
        ).join();

        int usersScore = usersFuture.join();
        int groupsScore = groupsFuture.join();
        int drivesScore = drivesFuture.join();
        int devicesScore = devicesFuture.join();
        int appAccessScore = appAccessFuture.join();
        int appPasswordsScore = appPasswordsFuture.join();
        int dnsScore = dnsAverageFuture.join();

        return new DashboardScores(usersScore, groupsScore, drivesScore,
                devicesScore, appAccessScore,appPasswordsScore, dnsScore);
    }

    private int calculateTotalScore(DashboardScores scores) {
        int totalCategories = 7;
        int totalScoresAdded = scores.usersScore() + scores.groupsScore() + scores.drivesScore() + scores.devicesScore() + scores.appAccessScore() + scores.appPasswordsScore()+scores.dnsScore();


        return Math.round((float)totalScoresAdded / totalCategories);
    }
}

