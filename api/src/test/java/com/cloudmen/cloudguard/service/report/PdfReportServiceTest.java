package com.cloudmen.cloudguard.service.report;

import com.cloudmen.cloudguard.domain.model.DnsRecordStatus;
import com.cloudmen.cloudguard.service.*;
import com.cloudmen.cloudguard.service.dns.DnsRecordsService;
import com.cloudmen.cloudguard.service.notification.NotificationAggregationService;
import com.cloudmen.cloudguard.service.preference.UserSecurityPreferenceService;
import com.cloudmen.cloudguard.service.teamleader.TeamleaderCompanyService;
import com.cloudmen.cloudguard.service.teamleader.TeamleaderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpHeaders;
import org.thymeleaf.TemplateEngine;

import java.util.List;
import java.util.Locale;
import java.util.Set;

import static com.cloudmen.cloudguard.utility.DashboardTestHelper.mockDomainDto;
import static com.cloudmen.cloudguard.utility.GlobalTestHelper.ADMIN;
import static com.cloudmen.cloudguard.utility.PdfReportTestHelper.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PdfReportServiceTest {
    @Mock private TemplateEngine templateEngine;
    @Mock private DashboardService dashboardService;
    @Mock private NotificationAggregationService notificationAggregationService;
    @Mock private GoogleUsersService googleUsersService;
    @Mock private GoogleGroupsService googleGroupsService;
    @Mock private GoogleSharedDriveService googleSharedDriveService;
    @Mock private GoogleDeviceService googleDeviceService;
    @Mock private GoogleOAuthService googleOAuthService;
    @Mock private AppPasswordsService appPasswordsService;
    @Mock private DnsRecordsService dnsRecordsService;
    @Mock private GoogleDomainService googleDomainService;
    @Mock private PasswordSettingsService passwordSettingsService;
    @Mock private TeamleaderCompanyService teamleaderCompanyService;
    @Mock private TeamleaderService teamleaderService;
    @Mock private MessageSource messageSource;
    @Mock private UserSecurityPreferenceService userSecurityPreferenceService;

    private PdfReportService pdfReportService;

    @BeforeEach
    void setUp() {
        pdfReportService = new PdfReportService(
                templateEngine, dashboardService, notificationAggregationService,
                googleUsersService, googleGroupsService, googleSharedDriveService,
                googleDeviceService, googleOAuthService, appPasswordsService,
                dnsRecordsService, googleDomainService, passwordSettingsService,
                teamleaderCompanyService, teamleaderService, messageSource, userSecurityPreferenceService
        );
    }

    @Test
    void generateSecurityRapport_success_mapsDataToContextCorrectly() {
        Locale locale = new Locale("nl");
        HttpHeaders mockHeaders = new HttpHeaders();
        Set<String> disabledPrefs = Set.of();

        when(teamleaderService.createHeaders()).thenReturn(mockHeaders);
        when(teamleaderCompanyService.getCompanyNameByEmail(ADMIN, mockHeaders)).thenReturn("Cloudmen BV");;
        when(userSecurityPreferenceService.getDisabledPreferenceKeys(ADMIN)).thenReturn(disabledPrefs);

        var dashboardMock = mockDashboardResponse(85);
        var notificationMock = mockNotification("Title", "Desc");
        var userMock = mockUserOverview(100, 10, 5, 2, 90);
        var groupMock = mockGroupOverview(50, 5, 1, 80);
        var driveMock = mockDriveOverview(20, 2, 0, 70);
        var deviceMock = mockDeviceOverview(100, 20, 10, 5, 80);
        var oAuthMock = mockOAuthOverview(50, 10, 70);
        var appPassMock = mockAppPasswordOverview(5, 3, 90);
        var passSettingsMock = mockPasswordSettings();

        when(dashboardService.getDashboardSecurityScores(ADMIN)).thenReturn(dashboardMock);
        when(notificationAggregationService.getCriticalNotifications(ADMIN, locale)).thenReturn(List.of(notificationMock));
        when(googleUsersService.getUsersPageOverview(ADMIN, disabledPrefs)).thenReturn(userMock);
        when(googleGroupsService.getGroupsOverview(ADMIN, disabledPrefs)).thenReturn(groupMock);
        when(googleSharedDriveService.getDrivesPageOverview(ADMIN, disabledPrefs)).thenReturn(driveMock);
        when(googleDeviceService.getDevicesPageOverview(ADMIN, disabledPrefs)).thenReturn(deviceMock);
        when(googleOAuthService.getOAuthPageOverview(ADMIN, disabledPrefs)).thenReturn(oAuthMock);
        when(appPasswordsService.getOverview(ADMIN, true, disabledPrefs)).thenReturn(appPassMock);
        when(passwordSettingsService.getPasswordSettings(ADMIN)).thenReturn(passSettingsMock);

        var domain = mockDomainDto("example.com");

        when(googleDomainService.getAllDomains(ADMIN)).thenReturn(List.of(domain));
        when(messageSource.getMessage(anyString(), any(), eq(locale))).thenReturn("Translated");
        var dnsRow = mockDnsRow("SPF", DnsRecordStatus.OK);
        var dnsMock = mockDnsResponse(100, List.of(dnsRow));
        when(dnsRecordsService.getImportantRecords(eq("example.com"), anyString(), any())).thenReturn(dnsMock);

    }
}
