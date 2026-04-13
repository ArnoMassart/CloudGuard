package com.cloudmen.cloudguard.integration.service.report;

import com.cloudmen.cloudguard.dto.apppasswords.AppPasswordOverviewResponse;
import com.cloudmen.cloudguard.dto.dashboard.DashboardPageResponse;
import com.cloudmen.cloudguard.dto.devices.DeviceOverviewResponse;
import com.cloudmen.cloudguard.dto.dns.DnsRecordResponseDto;
import com.cloudmen.cloudguard.dto.domain.DomainDto;
import com.cloudmen.cloudguard.dto.drives.SharedDriveOverviewResponse;
import com.cloudmen.cloudguard.dto.groups.GroupOverviewResponse;
import com.cloudmen.cloudguard.dto.oauth.OAuthOverviewResponse;
import com.cloudmen.cloudguard.dto.password.PasswordSettingsDto;
import com.cloudmen.cloudguard.dto.password.TwoStepVerificationDto;
import com.cloudmen.cloudguard.dto.report.ReportResponse;
import com.cloudmen.cloudguard.dto.users.UserOverviewResponse;
import com.cloudmen.cloudguard.exception.PdfGenerationException;
import com.cloudmen.cloudguard.service.*;
import com.cloudmen.cloudguard.service.dns.DnsRecordsService;
import com.cloudmen.cloudguard.service.notification.NotificationAggregationService;
import com.cloudmen.cloudguard.service.preference.UserSecurityPreferenceService;
import com.cloudmen.cloudguard.service.report.PdfReportService;
import com.cloudmen.cloudguard.service.teamleader.TeamleaderCompanyService;
import com.cloudmen.cloudguard.service.teamleader.TeamleaderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static com.cloudmen.cloudguard.unit.helper.GlobalTestHelper.ADMIN;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = {PdfReportService.class})
public class PdfReportServiceIT {

    @Autowired
    private PdfReportService pdfReportService;

    @MockitoBean private DashboardService dashboardService;
    @MockitoBean private NotificationAggregationService notificationAggregationService;
    @MockitoBean private GoogleUsersService googleUsersService;
    @MockitoBean private GoogleGroupsService googleGroupsService;
    @MockitoBean private GoogleSharedDriveService googleSharedDriveService;
    @MockitoBean private GoogleDeviceService googleDeviceService;
    @MockitoBean private GoogleOAuthService googleOAuthService;
    @MockitoBean private AppPasswordsService appPasswordsService;
    @MockitoBean private DnsRecordsService dnsRecordsService;
    @MockitoBean private GoogleDomainService googleDomainService;
    @MockitoBean private PasswordSettingsService passwordSettingsService;
    @MockitoBean private TeamleaderCompanyService teamleaderCompanyService;
    @MockitoBean private TeamleaderService teamleaderService;
    @MockitoBean private UserSecurityPreferenceService userSecurityPreferenceService;
    @MockitoBean private TemplateEngine templateEngine;

    @BeforeEach
    void setUp() {
        // Basis setup voor Teamleader (wordt buiten de try-catches aangeroepen)

        when(templateEngine.process(anyString(), any(Context.class)))
                .thenReturn("<html><body><h1>Test Report</h1></body></html>");

        when(teamleaderService.createHeaders()).thenReturn(new HttpHeaders());
        when(teamleaderCompanyService.getCompanyNameByEmail(anyString(), any()))
                .thenReturn("Cloudmen BV");

        // Mock Preferences (leeg setje disabled keys)
        when(userSecurityPreferenceService.getDisabledPreferenceKeys(anyString()))
                .thenReturn(Set.of());
        when(userSecurityPreferenceService.getDnsImportanceOverrides(anyString()))
                .thenReturn(Collections.emptyMap());
    }

    @Test
    void generateSecurityReport_success_generateValidPdfBytes() {
        Locale locale = new Locale("nl");

        var dashboardMock = mock(DashboardPageResponse.class);
        when(dashboardMock.overallScore()).thenReturn(85);
        when(dashboardService.getDashboardSecurityScores(ADMIN)).thenReturn(dashboardMock);

        when(notificationAggregationService.getCriticalNotifications(ADMIN, locale)).thenReturn(Collections.emptyList());

        var userMock = mock(UserOverviewResponse.class);
        when(userMock.totalUsers()).thenReturn(100);
        when(googleUsersService.getUsersPageOverview(eq(ADMIN), any())).thenReturn(userMock);

        var groupMock = mock(GroupOverviewResponse.class);
        when(googleGroupsService.getGroupsOverview(eq(ADMIN), any())).thenReturn(groupMock);

        var driveMock = mock(SharedDriveOverviewResponse.class);
        when(googleSharedDriveService.getDrivesPageOverview(eq(ADMIN), any())).thenReturn(driveMock);

        var deviceMock = mock(DeviceOverviewResponse.class);
        when(googleDeviceService.getDevicesPageOverview(eq(ADMIN), any())).thenReturn(deviceMock);

        var oAuthMock = mock(OAuthOverviewResponse.class);
        when(googleOAuthService.getOAuthPageOverview(eq(ADMIN), any())).thenReturn(oAuthMock);

        var appPassMock = mock(AppPasswordOverviewResponse.class);
        when(appPasswordsService.getOverview(eq(ADMIN), eq(true), any())).thenReturn(appPassMock);

        var passSettingsMock = mock(PasswordSettingsDto.class);
        when(passSettingsMock.twoStepVerification()).thenReturn(mock(TwoStepVerificationDto.class));
        when(passSettingsMock.adminsWithoutSecurityKeys()).thenReturn(Collections.emptyList());
        when(passSettingsMock.passwordPoliciesByOu()).thenReturn(Collections.emptyList());
        when(passwordSettingsService.getPasswordSettings(ADMIN)).thenReturn(passSettingsMock);

        var domainMock = mock(DomainDto.class);
        when(domainMock.domainName()).thenReturn("example.com");
        when(googleDomainService.getAllDomains(ADMIN)).thenReturn(List.of(domainMock));

        var dnsResponseMock = mock(DnsRecordResponseDto.class);
        when(dnsResponseMock.rows()).thenReturn(Collections.emptyList());
        when(dnsRecordsService.getImportantRecords(eq("example.com"), eq("google"), any())).thenReturn(dnsResponseMock);

        ReportResponse response = pdfReportService.generateSecurityReport(ADMIN, locale);

        assertNotNull(response);
        assertEquals("Cloudmen BV", response.companyName());
        assertNotNull(response.data());
        assertTrue(response.data().length > 1000);

        byte[] pdfPrefix = "%PDF-".getBytes();
        for (int i = 0; i < pdfPrefix.length; i++) {
            assertEquals(pdfPrefix[i], response.data()[i]);
        }
    }

    @Test
    void generateSecurityReport_withServiceExceptions_usesFallbacksAndGeneratesPdf() {
        Locale locale = new Locale("nl");

        when(dashboardService.getDashboardSecurityScores(ADMIN)).thenThrow(new RuntimeException("API down"));
        when(googleUsersService.getUsersPageOverview(eq(ADMIN), any())).thenThrow(new RuntimeException("API down"));
        when(googleGroupsService.getGroupsOverview(eq(ADMIN), any())).thenThrow(new RuntimeException("API down"));
        when(googleSharedDriveService.getDrivesPageOverview(eq(ADMIN), any())).thenThrow(new RuntimeException("API down"));
        when(googleDeviceService.getDevicesPageOverview(eq(ADMIN), any())).thenThrow(new RuntimeException("API down"));
        when(googleOAuthService.getOAuthPageOverview(eq(ADMIN), any())).thenThrow(new RuntimeException("API down"));
        when(appPasswordsService.getOverview(eq(ADMIN), eq(true), any())).thenThrow(new RuntimeException("API down"));
        when(passwordSettingsService.getPasswordSettings(ADMIN)).thenThrow(new RuntimeException("API down"));
        when(googleDomainService.getAllDomains(ADMIN)).thenThrow(new RuntimeException("API down"));

        ReportResponse response = pdfReportService.generateSecurityReport(ADMIN, locale);

        assertNotNull(response);
        assertNotNull(response.data());
        assertTrue(response.data().length > 0);
    }

    @Test
    void generateSecurityReport_teamleaderFails_throwsPdfGenerationException() {
        Locale locale = new Locale("nl");
        HttpHeaders mockHeaders = new HttpHeaders();
        when(teamleaderService.createHeaders()).thenReturn(mockHeaders);

        when(teamleaderCompanyService.getCompanyNameByEmail(ADMIN, mockHeaders))
                .thenThrow(new RuntimeException("Teamleader is offline"));

        PdfGenerationException exception = assertThrows(PdfGenerationException.class, () -> {
            pdfReportService.generateSecurityReport(ADMIN, locale);
        });

        assertTrue(exception.getMessage().contains("Error with pdf generation"));
    }
}
