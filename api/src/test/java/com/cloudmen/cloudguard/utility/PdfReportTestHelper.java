package com.cloudmen.cloudguard.utility;

import com.cloudmen.cloudguard.domain.model.DnsRecordStatus;
import com.cloudmen.cloudguard.dto.apppasswords.AppPasswordOverviewResponse;
import com.cloudmen.cloudguard.dto.dashboard.DashboardPageResponse;
import com.cloudmen.cloudguard.dto.devices.DeviceOverviewResponse;
import com.cloudmen.cloudguard.dto.dns.DnsRecordDto;
import com.cloudmen.cloudguard.dto.dns.DnsRecordResponseDto;
import com.cloudmen.cloudguard.dto.domain.DomainDto;
import com.cloudmen.cloudguard.dto.drives.SharedDriveOverviewResponse;
import com.cloudmen.cloudguard.dto.groups.GroupOverviewResponse;
import com.cloudmen.cloudguard.dto.notifications.NotificationDto;
import com.cloudmen.cloudguard.dto.oauth.OAuthOverviewResponse;
import com.cloudmen.cloudguard.dto.password.PasswordSettingsDto;
import com.cloudmen.cloudguard.dto.users.UserOverviewResponse;
import org.mockito.Mockito;

import java.util.List;

import static org.mockito.Mockito.*;

public class PdfReportTestHelper {
    public static DashboardPageResponse mockDashboardResponse(int score) {
        var mock = mock(DashboardPageResponse.class);
        lenient().when(mock.overallScore()).thenReturn(score);
        return mock;
    }

    public static NotificationDto mockNotification(String title, String desc) {
        var mock = mock(NotificationDto.class);
        lenient().when(mock.title()).thenReturn(title);
        lenient().when(mock.description()).thenReturn(desc);
        return mock;
    }

    public static UserOverviewResponse mockUserOverview(int total, int noMfa, int admins, int inactive, int score) {
        var mock = mock(UserOverviewResponse.class);
        lenient().when(mock.totalUsers()).thenReturn(total);
        lenient().when(mock.withoutTwoFactor()).thenReturn(noMfa);
        lenient().when(mock.adminUsers()).thenReturn(admins);
        lenient().when(mock.activeLongNoLoginCount()).thenReturn(inactive);
        lenient().when(mock.securityScore()).thenReturn(score);
        return mock;
    }

    public static GroupOverviewResponse mockGroupOverview(int total, int external, int highRisk, int score) {
        var mock = mock(GroupOverviewResponse.class);
        lenient().when(mock.totalGroups()).thenReturn(total);
        lenient().when(mock.groupsWithExternal()).thenReturn(external);
        lenient().when(mock.highRiskGroups()).thenReturn(highRisk);
        lenient().when(mock.securityScore()).thenReturn(score);
        return mock;
    }

    public static SharedDriveOverviewResponse mockDriveOverview(int total, int outsideDomain, int orphans, int score) {
        var mock = mock(SharedDriveOverviewResponse.class);
        lenient().when(mock.totalDrives()).thenReturn(total);
        lenient().when(mock.notOnlyDomainUsersAllowedCount()).thenReturn(outsideDomain);
        lenient().when(mock.orphanDrives()).thenReturn(orphans);
        lenient().when(mock.securityScore()).thenReturn(score);
        return mock;
    }

    public static DeviceOverviewResponse mockDeviceOverview(int total, int nonCompliant, int encryptionCount, int osCount, int score) {
        var mock = mock(DeviceOverviewResponse.class);
        lenient().when(mock.totalDevices()).thenReturn(total);
        lenient().when(mock.totalNonCompliant()).thenReturn(nonCompliant);
        lenient().when(mock.encryptionCount()).thenReturn(encryptionCount);
        lenient().when(mock.osVersionCount()).thenReturn(osCount);
        lenient().when(mock.securityScore()).thenReturn(score);
        return mock;
    }

    public static OAuthOverviewResponse mockOAuthOverview(int total, int highRisk, int score) {
        var mock = mock(OAuthOverviewResponse.class);
        lenient().when(mock.totalThirdPartyApps()).thenReturn((long) total);
        lenient().when(mock.totalHighRiskApps()).thenReturn((long) highRisk);
        lenient().when(mock.securityScore()).thenReturn(score);
        return mock;
    }

    public static AppPasswordOverviewResponse mockAppPasswordOverview(int total, int users, int score) {
        var mock = mock(AppPasswordOverviewResponse.class);
        lenient().when(mock.totalAppPasswords()).thenReturn(total);
        lenient().when(mock.usersWithAppPasswords()).thenReturn(users);
        lenient().when(mock.securityScore()).thenReturn(score);
        return mock;
    }

    public static PasswordSettingsDto mockPasswordSettings() {
        // Gezien de complexiteit van de inner classes bij password settings mocken we deze heel oppervlakkig
        var mock = mock(PasswordSettingsDto.class);
        lenient().when(mock.securityScore()).thenReturn(90);

        var twoStepMock = mock(com.cloudmen.cloudguard.dto.password.TwoStepVerificationDto.class);
        lenient().when(mock.twoStepVerification()).thenReturn(twoStepMock);
        lenient().when(twoStepMock.byOrgUnit()).thenReturn(List.of());

        lenient().when(mock.passwordPoliciesByOu()).thenReturn(List.of());
        lenient().when(mock.adminsWithoutSecurityKeys()).thenReturn(List.of());
        return mock;
    }

    public static DomainDto mockDomain(String name) {
        var mock = mock(DomainDto.class);
        lenient().when(mock.domainName()).thenReturn(name);
        return mock;
    }

    public static DnsRecordResponseDto mockDnsResponse(int score, List<DnsRecordDto> rows) {
        var mock = mock(DnsRecordResponseDto.class);
        lenient().when(mock.securityScore()).thenReturn(score);
        lenient().when(mock.rows()).thenReturn(rows);
        return mock;
    }

    public static DnsRecordDto mockDnsRow(String type, DnsRecordStatus status) {
        var mock = mock(DnsRecordDto.class);
        lenient().when(mock.type()).thenReturn(type);
        lenient().when(mock.status()).thenReturn(status);
        lenient().when(mock.values()).thenReturn(List.of("value1"));
        return mock;
    }
}
