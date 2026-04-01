package com.cloudmen.cloudguard.unit.helper;

import com.cloudmen.cloudguard.dto.apppasswords.AppPasswordOverviewResponse;
import com.cloudmen.cloudguard.dto.devices.DeviceOverviewResponse;
import com.cloudmen.cloudguard.dto.domain.DomainDto;
import com.cloudmen.cloudguard.dto.drives.SharedDriveOverviewResponse;
import com.cloudmen.cloudguard.dto.groups.GroupOverviewResponse;
import com.cloudmen.cloudguard.dto.oauth.OAuthOverviewResponse;
import com.cloudmen.cloudguard.dto.password.PasswordSettingsDto;
import com.cloudmen.cloudguard.dto.users.UserOverviewResponse;
import org.mockito.Mock;
import org.mockito.Mockito;

import static org.mockito.Mockito.when;

public class DashboardTestHelper {
    public static UserOverviewResponse mockUserResponse(int score) {
        var mock = Mockito.mock(UserOverviewResponse.class);
        when(mock.securityScore()).thenReturn(score);
        return mock;
    }

    public static GroupOverviewResponse mockGroupResponse(int score) {
        var mock = Mockito.mock(GroupOverviewResponse.class);
        when(mock.securityScore()).thenReturn(score);
        return mock;
    }

    public static SharedDriveOverviewResponse mockDriveResponse(int score) {
        var mock = Mockito.mock(SharedDriveOverviewResponse.class);
        when(mock.securityScore()).thenReturn(score);
        return mock;
    }

    public static DeviceOverviewResponse mockDeviceResponse(int score) {
        var mock = Mockito.mock(DeviceOverviewResponse.class);
        when(mock.securityScore()).thenReturn(score);
        return mock;
    }

    public static OAuthOverviewResponse mockOAuthResponse(int score) {
        var mock = Mockito.mock(OAuthOverviewResponse.class);
        when(mock.securityScore()).thenReturn(score);
        return mock;
    }

    public static AppPasswordOverviewResponse mockAppPasswordResponse(int score) {
        var mock = Mockito.mock(AppPasswordOverviewResponse.class);
        when(mock.securityScore()).thenReturn(score);
        return mock;
    }

    public static PasswordSettingsDto mockPasswordSettingsResponse(int score) {
        var mock = Mockito.mock(PasswordSettingsDto.class);
        when(mock.securityScore()).thenReturn(score);
        return mock;
    }

    // Aangenomen dat DomainDto een record is met een domainName() methode.
    // Pas dit aan als jouw class er net iets anders uitziet!
    public static DomainDto mockDomainDto(String name) {
        var mock = Mockito.mock(DomainDto.class);
        when(mock.domainName()).thenReturn(name);
        return mock;
    }
}
