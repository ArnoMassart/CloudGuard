package com.cloudmen.cloudguard.unit.service.report;

import com.cloudmen.cloudguard.domain.model.DnsRecordStatus;
import com.cloudmen.cloudguard.domain.model.Organization;
import com.cloudmen.cloudguard.domain.model.User;
import com.cloudmen.cloudguard.domain.model.UserRole;
import com.cloudmen.cloudguard.dto.report.FullSecurityReport;
import com.cloudmen.cloudguard.dto.report.ReportResponse;
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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpHeaders;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

import static com.cloudmen.cloudguard.unit.helper.DashboardTestHelper.mockDomainDto;
import static com.cloudmen.cloudguard.unit.helper.GlobalTestHelper.ADMIN;
import static com.cloudmen.cloudguard.unit.helper.PdfReportTestHelper.*;
import static org.junit.jupiter.api.Assertions.*;
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
    @Mock private UserService userService;
    @Mock private OrganizationService organizationService;

    private PdfReportService pdfReportService;

    @BeforeEach
    void setUp() {
        pdfReportService = new PdfReportService(
                templateEngine, dashboardService, notificationAggregationService,
                googleUsersService, googleGroupsService, googleSharedDriveService,
                googleDeviceService, googleOAuthService, appPasswordsService,
                dnsRecordsService, googleDomainService, passwordSettingsService,
                teamleaderCompanyService, teamleaderService, messageSource,
                userSecurityPreferenceService, userService, organizationService
        );

        // 1. Maak een mock Organisatie aan
        Organization org = new Organization();
        org.setId(1L);
        org.setAdminEmail(ADMIN);

        // 2. Geef de mock Gebruiker toegang én koppel hem aan de organisatie (Id = 1)
        User adminUser = new User();
        adminUser.setEmail(ADMIN);
        adminUser.setOrganizationId(1L);
        adminUser.setRoles(List.of(UserRole.SUPER_ADMIN));

        // 3. Gebruik anyString() en anyLong() zodat de HelperMethod ALTIJD slaagt,
        // ongeacht met welk e-mailadres de test hem aanroept.
        lenient().when(userService.findByEmailOptional(anyString())).thenReturn(Optional.of(adminUser));
        lenient().when(organizationService.findById(anyLong())).thenReturn(Optional.of(org));

        // Voor de tests die specifieke rollen nodig hebben (zoals de 'noAccess' test),
        // kun je in de specifieke @Test nog steeds de findByEmail (zonder Optional) overschrijven,
        // want die wordt door getFullSecurityReport rechtstreeks gebruikt.
        lenient().when(userService.findByEmail(anyString())).thenReturn(adminUser);
    }

    @Test
    void generateSecurityReport_success_mapsDataToContextCorrectly() {
        Locale locale = new Locale("nl");
        HttpHeaders mockHeaders = new HttpHeaders();
        Set<String> disabledPrefs = Set.of();

        when(teamleaderService.createHeaders()).thenReturn(mockHeaders);
        when(teamleaderCompanyService.getCompanyNameByEmail(ADMIN, mockHeaders)).thenReturn("Cloudmen BV");
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

        when(appPasswordsService.getOverview(ADMIN, false, disabledPrefs)).thenReturn(appPassMock);
        when(passwordSettingsService.getPasswordSettings(ADMIN)).thenReturn(passSettingsMock);

        var domain = mockDomainDto("example.com");
        when(googleDomainService.getAllDomains(ADMIN)).thenReturn(List.of(domain));
        when(messageSource.getMessage(anyString(), any(), eq(locale))).thenReturn("Translated");
        var dnsRow = mockDnsRow("SPF", DnsRecordStatus.OK);
        var dnsMock = mockDnsResponse(100, List.of(dnsRow));
        when(dnsRecordsService.getImportantRecords(eq("example.com"), anyString(), any())).thenReturn(dnsMock);

        when(templateEngine.process(anyString(), any(Context.class))).thenReturn("<html><body>Test Report</body></html>");

        ReportResponse response = pdfReportService.generateSecurityReport(ADMIN, locale);

        assertEquals("Cloudmen BV", response.companyName());
        assertNotNull(response.data());
        assertTrue(response.data().length > 0);

        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        verify(templateEngine).process(eq("security-template_nl"), contextCaptor.capture());

        Context usedContext = contextCaptor.getValue();
        FullSecurityReport report = (FullSecurityReport) usedContext.getVariable("report");

        assertTrue((Boolean) usedContext.getVariable("showUsersGroups"));
        assertTrue((Boolean) usedContext.getVariable("showDrives"));

        assertEquals(85, report.overallScore());
        assertEquals(1, report.criticalRisks().size());
        assertEquals(100, report.users().total());
        assertEquals(90, report.users().mfaPct());
        assertFalse(report.users().hasError());
    }

    @Test
    void generateSecurityReport_withServiceExceptions_usesFallbacksAndSetsIsError() {
        Locale locale = new Locale("nl");
        HttpHeaders mockHeaders = new HttpHeaders();
        Set<String> disabledPrefs = Set.of();

        when(teamleaderService.createHeaders()).thenReturn(mockHeaders);
        when(teamleaderCompanyService.getCompanyNameByEmail(ADMIN, mockHeaders)).thenReturn("Error Corp");
        when(userSecurityPreferenceService.getDisabledPreferenceKeys(ADMIN)).thenReturn(disabledPrefs);

        when(dashboardService.getDashboardSecurityScores(ADMIN)).thenThrow(new RuntimeException("API down"));
        when(googleUsersService.getUsersPageOverview(ADMIN, disabledPrefs)).thenThrow(new RuntimeException("API down"));
        when(googleGroupsService.getGroupsOverview(ADMIN, disabledPrefs)).thenThrow(new RuntimeException("API down"));
        when(googleSharedDriveService.getDrivesPageOverview(ADMIN, disabledPrefs)).thenThrow(new RuntimeException("API down"));
        when(googleDeviceService.getDevicesPageOverview(ADMIN, disabledPrefs)).thenThrow(new RuntimeException("API down"));
        when(googleOAuthService.getOAuthPageOverview(ADMIN, disabledPrefs)).thenThrow(new RuntimeException("API down"));
        when(appPasswordsService.getOverview(ADMIN, false, disabledPrefs)).thenThrow(new RuntimeException("API down"));
        when(passwordSettingsService.getPasswordSettings(ADMIN)).thenThrow(new RuntimeException("API down"));
        when(googleDomainService.getAllDomains(ADMIN)).thenThrow(new RuntimeException("API down"));

        when(templateEngine.process(anyString(), any(Context.class))).thenReturn("<html><body>Fallback Report</body></html>");

        ReportResponse response = pdfReportService.generateSecurityReport(ADMIN, locale);

        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        verify(templateEngine).process(eq("security-template_nl"), contextCaptor.capture());

        FullSecurityReport reportData = (FullSecurityReport) contextCaptor.getValue().getVariable("report");

        assertEquals(100, reportData.overallScore());
        assertTrue(reportData.users().hasError());
        assertTrue(reportData.groups().hasError());
        assertTrue(reportData.drives().hasError());
        assertTrue(reportData.devices().hasError());
        assertTrue(reportData.appAccess().hasError());
        assertTrue(reportData.appPasswords().hasError());
        assertTrue(reportData.passwordSettings().hasError());
        assertTrue(reportData.domains().isEmpty());
    }

    @Test
    void generateSecurityReport_templateEngineFails_throwsPdfGenerationException() {
        Locale locale = new Locale("nl");
        HttpHeaders mockHeaders = new HttpHeaders();

        // Gebruik lenient() om UnnecessaryStubbingExceptions te voorkomen omdat de templateEngine crashed
        lenient().when(teamleaderService.createHeaders()).thenReturn(mockHeaders);
        lenient().when(teamleaderCompanyService.getCompanyNameByEmail(ADMIN, mockHeaders)).thenReturn("Cloudmen");

        when(templateEngine.process(anyString(), any(Context.class))).thenThrow(new RuntimeException("Thymeleaf parsing error"));

        PdfGenerationException exception = assertThrows(PdfGenerationException.class, () -> {
            pdfReportService.generateSecurityReport(ADMIN, locale);
        });

        assertTrue(exception.getMessage().contains("Error with pdf generation"));
    }
}