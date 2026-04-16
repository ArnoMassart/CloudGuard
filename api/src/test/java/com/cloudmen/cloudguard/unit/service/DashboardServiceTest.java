package com.cloudmen.cloudguard.unit.service;

import com.cloudmen.cloudguard.domain.model.User;
import com.cloudmen.cloudguard.domain.model.UserRole;
import com.cloudmen.cloudguard.dto.dashboard.DashboardOverviewResponse;
import com.cloudmen.cloudguard.dto.dashboard.DashboardPageResponse;
import com.cloudmen.cloudguard.service.*;
import com.cloudmen.cloudguard.service.dns.DnsRecordsService;
import com.cloudmen.cloudguard.service.notification.NotificationAggregationService;
import com.cloudmen.cloudguard.service.preference.UserSecurityPreferenceService;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static com.cloudmen.cloudguard.unit.helper.DashboardTestHelper.*;
import static com.cloudmen.cloudguard.unit.helper.GlobalTestHelper.ADMIN;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DashboardServiceTest {
    @Mock private GoogleUsersService usersService;
    @Mock private GoogleGroupsService groupsService;
    @Mock private GoogleSharedDriveService sharedDriveService;
    @Mock private GoogleDeviceService googleDeviceService;
    @Mock private GoogleOAuthService oAuthService;
    @Mock private AppPasswordsService passwordsService;
    @Mock private DnsRecordsService dnsRecordsService;
    @Mock private GoogleDomainService domainService;
    @Mock private NotificationAggregationService notificationService;
    @Mock private PasswordSettingsService passwordSettingsService;
    @Mock private UserSecurityPreferenceService userSecurityPreferenceService;
    @Mock private UserService userService;

    private DashboardService dashboardService;

    @BeforeEach
    void setUp() {
        dashboardService = new DashboardService(
                usersService, groupsService, sharedDriveService, googleDeviceService,
                oAuthService, passwordsService, dnsRecordsService, domainService,
                notificationService, passwordSettingsService, userSecurityPreferenceService, userService
        );
    }

    // Helper methode om snel een admin gebruiker te mocken
    private User createMockAdminUser() {
        User user = new User();
        user.setEmail(ADMIN);
        user.setRoles(List.of(UserRole.SUPER_ADMIN));
        return user;
    }

    @Test
    void getDashboardOverview_success_returnsCounts() {
        when(notificationService.getNotificationsCount(ADMIN)).thenReturn(15L);
        when(notificationService.getNotificationsCriticalCount(ADMIN)).thenReturn(3L);

        DashboardOverviewResponse response = dashboardService.getDashboardOverview(ADMIN);

        assertEquals(15, response.totalNotifications());
        assertEquals(3, response.criticalNotifications());
    }

    @Test
    void getDashboardOverview_exception_returnsZeroesWithoutCrashing() {
        when(notificationService.getNotificationsCount(ADMIN)).thenThrow(new RuntimeException("Database error"));

        DashboardOverviewResponse response = dashboardService.getDashboardOverview(ADMIN);

        assertEquals(0, response.totalNotifications());
        assertEquals(0, response.criticalNotifications());
    }

    @Test
    void getDashboardSecurityScores_success_calculatesAverageScore() {
        // Zorg dat de service denkt dat de ingelogde gebruiker een SUPER_ADMIN is
        when(userService.findByEmail(ADMIN)).thenReturn(createMockAdminUser());

        Set<String> disabledPrefs = Set.of();
        when(userSecurityPreferenceService.getDisabledPreferenceKeys(ADMIN)).thenReturn(disabledPrefs);
        when(userSecurityPreferenceService.getDnsImportanceOverrides(ADMIN)).thenReturn(null);

        var userMock = mockUserResponse(80);
        var groupMock = mockGroupResponse(90);
        var driveMock = mockDriveResponse(70);
        var deviceMock = mockDeviceResponse(100);
        var oauthMock = mockOAuthResponse(60);
        var passwordMock = mockAppPasswordResponse(50);
        var passwordSettingsMock = mockPasswordSettingsResponse(90);

        when(usersService.getUsersPageOverview(ADMIN, disabledPrefs)).thenReturn(userMock);
        when(groupsService.getGroupsOverview(ADMIN, disabledPrefs)).thenReturn(groupMock);
        when(sharedDriveService.getDrivesPageOverview(ADMIN, disabledPrefs)).thenReturn(driveMock);
        when(googleDeviceService.getDevicesPageOverview(ADMIN, disabledPrefs)).thenReturn(deviceMock);
        when(oAuthService.getOAuthPageOverview(ADMIN, disabledPrefs)).thenReturn(oauthMock);
        when(passwordsService.getOverview(eq(ADMIN), anyBoolean(), eq(disabledPrefs))).thenReturn(passwordMock);
        when(passwordSettingsService.getPasswordSettings(ADMIN)).thenReturn(passwordSettingsMock);

        var domain1 = mockDomainDto("domain1.com");
        var domain2 = mockDomainDto("domain2.com");
        when(domainService.getAllDomains(ADMIN)).thenReturn(List.of(domain1, domain2));

        var dnsMock1 = mock(com.cloudmen.cloudguard.dto.dns.DnsRecordResponseDto.class);
        when(dnsMock1.securityScore()).thenReturn(100);
        var dnsMock2 = mock(com.cloudmen.cloudguard.dto.dns.DnsRecordResponseDto.class);
        when(dnsMock2.securityScore()).thenReturn(80);

        when(dnsRecordsService.getImportantRecords(eq("domain1.com"), anyString(), any())).thenReturn(dnsMock1);
        when(dnsRecordsService.getImportantRecords(eq("domain2.com"), anyString(), any())).thenReturn(dnsMock2);

        DashboardPageResponse response = dashboardService.getDashboardSecurityScores(ADMIN);

        assertEquals(79, response.overallScore());
        assertEquals(80, response.scores().usersScore());
        assertEquals(90, response.scores().dnsScore());
        assertNotNull(response.lastUpdated());
    }

    @Test
    void getDashboardSecurityScores_withExceptions_usesFallbackScores() {
        when(userService.findByEmail(ADMIN)).thenReturn(createMockAdminUser());

        Set<String> disabledPrefs = Set.of();
        when(userSecurityPreferenceService.getDisabledPreferenceKeys(ADMIN)).thenReturn(disabledPrefs);

        when(usersService.getUsersPageOverview(ADMIN, disabledPrefs)).thenThrow(new RuntimeException("API plat"));
        when(groupsService.getGroupsOverview(ADMIN, disabledPrefs)).thenThrow(new RuntimeException("Timeout"));
        when(sharedDriveService.getDrivesPageOverview(ADMIN, disabledPrefs)).thenThrow(new RuntimeException("Error"));
        when(googleDeviceService.getDevicesPageOverview(ADMIN, disabledPrefs)).thenThrow(new RuntimeException("Error"));
        when(oAuthService.getOAuthPageOverview(ADMIN, disabledPrefs)).thenThrow(new RuntimeException("Error"));
        when(passwordsService.getOverview(eq(ADMIN), anyBoolean(), eq(disabledPrefs))).thenThrow(new RuntimeException("Error"));
        when(passwordSettingsService.getPasswordSettings(ADMIN)).thenThrow(new RuntimeException("Error"));
        when(domainService.getAllDomains(ADMIN)).thenThrow(new RuntimeException("DNS service down"));

        DashboardPageResponse response = dashboardService.getDashboardSecurityScores(ADMIN);

        assertEquals(100, response.scores().usersScore());
        assertEquals(100, response.scores().groupsScore());
        assertEquals(100, response.scores().dnsScore());
        assertEquals(100, response.overallScore());
    }

    @Test
    void getDashboardSecurityScores_noDomains_dnsReturnsZero() {
        when(userService.findByEmail(ADMIN)).thenReturn(createMockAdminUser());

        Set<String> disabledPrefs = Set.of();
        when(userSecurityPreferenceService.getDisabledPreferenceKeys(ADMIN)).thenReturn(disabledPrefs);

        // 1. Maak EERST alle mock-objecten aan (dit voorkomt de UnfinishedStubbingException!)
        var userMock = mockUserResponse(50);
        var groupMock = mockGroupResponse(50);
        var driveMock = mockDriveResponse(50);
        var deviceMock = mockDeviceResponse(50);
        var oauthMock = mockOAuthResponse(50);
        var passwordMock = mockAppPasswordResponse(50);
        var passwordSettingsMock = mockPasswordSettingsResponse(50);

        // 2. Koppel ze DAARNA pas aan de thenReturn()
        when(usersService.getUsersPageOverview(ADMIN, disabledPrefs)).thenReturn(userMock);
        when(groupsService.getGroupsOverview(ADMIN, disabledPrefs)).thenReturn(groupMock);
        when(sharedDriveService.getDrivesPageOverview(ADMIN, disabledPrefs)).thenReturn(driveMock);
        when(googleDeviceService.getDevicesPageOverview(ADMIN, disabledPrefs)).thenReturn(deviceMock);
        when(oAuthService.getOAuthPageOverview(ADMIN, disabledPrefs)).thenReturn(oauthMock);
        when(passwordsService.getOverview(eq(ADMIN), anyBoolean(), eq(disabledPrefs))).thenReturn(passwordMock);
        when(passwordSettingsService.getPasswordSettings(ADMIN)).thenReturn(passwordSettingsMock);

        when(domainService.getAllDomains(ADMIN)).thenReturn(List.of());

        DashboardPageResponse response = dashboardService.getDashboardSecurityScores(ADMIN);

        assertEquals(0, response.scores().dnsScore());
        assertEquals(44, response.overallScore());
    }

    // --- NIEUWE TEST ---
    // Controleert of een gebruiker zonder rechten netjes -1 terugkrijgt voor alles, en een gemiddelde van 0.
    @Test
    void getDashboardSecurityScores_noAccess_returnsMinusOnes() {
        // Maak een gebruiker zonder enige nuttige rol
        User unassignedUser = new User();
        unassignedUser.setEmail("viewer@cloudguard.com");
        unassignedUser.setRoles(List.of(UserRole.UNASSIGNED));

        when(userService.findByEmail("viewer@cloudguard.com")).thenReturn(unassignedUser);

        DashboardPageResponse response = dashboardService.getDashboardSecurityScores("viewer@cloudguard.com");

        // Alle scores moeten door de "fetchScoreIfAllowed" naar -1 worden gezet
        assertEquals(-1, response.scores().usersScore());
        assertEquals(-1, response.scores().groupsScore());
        assertEquals(-1, response.scores().drivesScore());
        assertEquals(-1, response.scores().dnsScore());

        // Het totaal gemiddelde moet 0 zijn (voorkomt division by zero in calculateTotalScore)
        assertEquals(0, response.overallScore());

        // Omdat alle rechten ontbreken, zou GEEN ENKELE API service mogen worden aangeroepen
        verify(usersService, never()).getUsersPageOverview(anyString(), anySet());
        verify(domainService, never()).getAllDomains(anyString());
    }
}