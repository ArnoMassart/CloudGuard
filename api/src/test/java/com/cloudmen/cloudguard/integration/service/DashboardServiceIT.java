package com.cloudmen.cloudguard.integration.service;

import com.cloudmen.cloudguard.dto.dashboard.DashboardOverviewResponse;
import com.cloudmen.cloudguard.dto.dashboard.DashboardPageResponse;
import com.cloudmen.cloudguard.dto.devices.DeviceOverviewResponse;
import com.cloudmen.cloudguard.dto.dns.DnsRecordResponseDto;
import com.cloudmen.cloudguard.dto.domain.DomainDto;
import com.cloudmen.cloudguard.dto.drives.SharedDriveOverviewResponse;
import com.cloudmen.cloudguard.dto.groups.GroupOverviewResponse;
import com.cloudmen.cloudguard.dto.oauth.OAuthOverviewResponse;
import com.cloudmen.cloudguard.dto.apppasswords.AppPasswordOverviewResponse;
import com.cloudmen.cloudguard.dto.password.PasswordSettingsDto;
import com.cloudmen.cloudguard.dto.users.UserOverviewResponse;
import com.cloudmen.cloudguard.service.*;
import com.cloudmen.cloudguard.service.dns.DnsRecordsService;
import com.cloudmen.cloudguard.service.notification.NotificationAggregationService;
import com.cloudmen.cloudguard.service.preference.UserSecurityPreferenceService;
import com.cloudmen.cloudguard.utility.GoogleApiFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = {DashboardService.class})
public class DashboardServiceIT {

    @Autowired
    private DashboardService dashboardService;

    @MockitoBean private GoogleUsersService usersService;
    @MockitoBean private GoogleGroupsService groupsService;
    @MockitoBean private GoogleSharedDriveService sharedDriveService;
    @MockitoBean private GoogleDeviceService googleDeviceService;
    @MockitoBean private GoogleOAuthService oAuthService;
    @MockitoBean private AppPasswordsService passwordsService;
    @MockitoBean private DnsRecordsService dnsRecordsService;
    @MockitoBean private GoogleDomainService domainService;
    @MockitoBean private NotificationAggregationService notificationService;
    @MockitoBean private PasswordSettingsService passwordSettingsService;
    @MockitoBean private UserSecurityPreferenceService userSecurityPreferenceService;
    @MockitoBean
    private GoogleApiFactory googleApiFactory;

    @MockitoBean
    private AdminSecurityKeysService adminSecurityKeysService;

    private static final String EMAIL = "admin@cloudmen.com";

    @BeforeEach
    void setUp() {
        when(userSecurityPreferenceService.getDisabledPreferenceKeys(EMAIL)).thenReturn(Set.of());
        when(userSecurityPreferenceService.getDnsImportanceOverrides(EMAIL)).thenReturn(Collections.emptyMap());
    }

    @Test
    void getDashboardSecurityScores_success_calculatesCorrectAverage() {
        var userMock = mock(UserOverviewResponse.class);
        when(userMock.securityScore()).thenReturn(80);
        when(usersService.getUsersPageOverview(eq(EMAIL), any())).thenReturn(userMock);

        var groupMock = mock(GroupOverviewResponse.class);
        when(groupMock.securityScore()).thenReturn(70);
        when(groupsService.getGroupsOverview(eq(EMAIL), any())).thenReturn(groupMock);

        var driveMock = mock(SharedDriveOverviewResponse.class);
        when(driveMock.securityScore()).thenReturn(90);
        when(sharedDriveService.getDrivesPageOverview(eq(EMAIL), any())).thenReturn(driveMock);

        var deviceMock = mock(DeviceOverviewResponse.class);
        when(deviceMock.securityScore()).thenReturn(60);
        when(googleDeviceService.getDevicesPageOverview(eq(EMAIL), any())).thenReturn(deviceMock);

        var oAuthMock = mock(OAuthOverviewResponse.class);
        when(oAuthMock.securityScore()).thenReturn(100);
        when(oAuthService.getOAuthPageOverview(eq(EMAIL), any())).thenReturn(oAuthMock);

        var appPassMock = mock(AppPasswordOverviewResponse.class);
        when(appPassMock.securityScore()).thenReturn(85);
        when(passwordsService.getOverview(eq(EMAIL), anyBoolean(), any())).thenReturn(appPassMock);

        var passSettingsMock = mock(PasswordSettingsDto.class);
        when(passSettingsMock.securityScore()).thenReturn(75);
        when(passwordSettingsService.getPasswordSettings(EMAIL)).thenReturn(passSettingsMock);

        var domainMock1 = mock(DomainDto.class);
        when(domainMock1.domainName()).thenReturn("test1.com");
        var domainMock2 = mock(DomainDto.class);
        when(domainMock2.domainName()).thenReturn("test2.com");
        when(domainService.getAllDomains(EMAIL)).thenReturn(List.of(domainMock1, domainMock2));

        var dnsMock1 = mock(DnsRecordResponseDto.class);
        when(dnsMock1.securityScore()).thenReturn(50);
        when(dnsRecordsService.getImportantRecords(eq("test1.com"), eq("google"), any())).thenReturn(dnsMock1);

        var dnsMock2 = mock(DnsRecordResponseDto.class);
        when(dnsMock2.securityScore()).thenReturn(100);
        when(dnsRecordsService.getImportantRecords(eq("test2.com"), eq("google"), any())).thenReturn(dnsMock2);

        DashboardPageResponse response = dashboardService.getDashboardSecurityScores(EMAIL);

        assertNotNull(response);
        assertEquals(80, response.scores().usersScore());
        assertEquals(70, response.scores().groupsScore());
        assertEquals(90, response.scores().drivesScore());
        assertEquals(60, response.scores().devicesScore());
        assertEquals(100, response.scores().appAccessScore());
        assertEquals(85, response.scores().appPasswordsScore());
        assertEquals(75, response.scores().passwordSettingsScore());
        assertEquals(75, response.scores().dnsScore());

        int expectedTotal = Math.round((80 + 70 + 90 + 60 + 100 + 85 + 75 + 75) / 8.0f);
        assertEquals(expectedTotal, response.overallScore());
        assertNotNull(response.lastUpdated());
    }

    @Test
    void getDashboardSecurityScores_withExceptions_usesFallbacks() {
        when(usersService.getUsersPageOverview(anyString(), any())).thenThrow(new RuntimeException("Error"));
        when(groupsService.getGroupsOverview(anyString(), any())).thenThrow(new RuntimeException("Error"));
        when(sharedDriveService.getDrivesPageOverview(anyString(), any())).thenThrow(new RuntimeException("Error"));
        when(googleDeviceService.getDevicesPageOverview(anyString(), any())).thenThrow(new RuntimeException("Error"));
        when(oAuthService.getOAuthPageOverview(anyString(), any())).thenThrow(new RuntimeException("Error"));
        when(passwordsService.getOverview(anyString(), anyBoolean(), any())).thenThrow(new RuntimeException("Error"));
        when(passwordSettingsService.getPasswordSettings(anyString())).thenThrow(new RuntimeException("Error"));
        when(domainService.getAllDomains(anyString())).thenThrow(new RuntimeException("Error"));

        DashboardPageResponse response = dashboardService.getDashboardSecurityScores(EMAIL);

        assertNotNull(response);
        assertEquals(100, response.scores().usersScore());
        assertEquals(100, response.scores().groupsScore());
        assertEquals(100, response.scores().drivesScore());
        assertEquals(100, response.scores().devicesScore());
        assertEquals(100, response.scores().appAccessScore());
        assertEquals(100, response.scores().appPasswordsScore());
        assertEquals(100, response.scores().passwordSettingsScore());
        assertEquals(100, response.scores().dnsScore());
        assertEquals(100, response.overallScore());
    }

    @Test
    void getDashboardSecurityScores_noDomains_dnsScoreIsZero() {
        var userMock = mock(UserOverviewResponse.class);
        when(userMock.securityScore()).thenReturn(100);
        when(usersService.getUsersPageOverview(eq(EMAIL), any())).thenReturn(userMock);

        var groupMock = mock(GroupOverviewResponse.class);
        when(groupMock.securityScore()).thenReturn(100);
        when(groupsService.getGroupsOverview(eq(EMAIL), any())).thenReturn(groupMock);

        var driveMock = mock(SharedDriveOverviewResponse.class);
        when(driveMock.securityScore()).thenReturn(100);
        when(sharedDriveService.getDrivesPageOverview(eq(EMAIL), any())).thenReturn(driveMock);

        var deviceMock = mock(DeviceOverviewResponse.class);
        when(deviceMock.securityScore()).thenReturn(100);
        when(googleDeviceService.getDevicesPageOverview(eq(EMAIL), any())).thenReturn(deviceMock);

        var oAuthMock = mock(OAuthOverviewResponse.class);
        when(oAuthMock.securityScore()).thenReturn(100);
        when(oAuthService.getOAuthPageOverview(eq(EMAIL), any())).thenReturn(oAuthMock);

        var appPassMock = mock(AppPasswordOverviewResponse.class);
        when(appPassMock.securityScore()).thenReturn(100);
        when(passwordsService.getOverview(eq(EMAIL), anyBoolean(), any())).thenReturn(appPassMock);

        var passSettingsMock = mock(PasswordSettingsDto.class);
        when(passSettingsMock.securityScore()).thenReturn(100);
        when(passwordSettingsService.getPasswordSettings(EMAIL)).thenReturn(passSettingsMock);

        when(domainService.getAllDomains(EMAIL)).thenReturn(Collections.emptyList());

        DashboardPageResponse response = dashboardService.getDashboardSecurityScores(EMAIL);

        assertEquals(0, response.scores().dnsScore());
    }

    @Test
    void getDashboardOverview_success_returnsCounts() {
        when(notificationService.getNotificationsCount(EMAIL)).thenReturn(15L);
        when(notificationService.getNotificationsCriticalCount(EMAIL)).thenReturn(3L);

        DashboardOverviewResponse response = dashboardService.getDashboardOverview(EMAIL);

        assertNotNull(response);
        assertEquals(15, response.totalNotifications());
        assertEquals(3, response.criticalNotifications());
    }

    @Test
    void getDashboardOverview_exception_returnsZeroCounts() {
        when(notificationService.getNotificationsCount(EMAIL)).thenThrow(new RuntimeException("Database down"));

        DashboardOverviewResponse response = dashboardService.getDashboardOverview(EMAIL);

        assertNotNull(response);
        assertEquals(0, response.totalNotifications());
        assertEquals(0, response.criticalNotifications());
    }
}