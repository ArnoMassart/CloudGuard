package com.cloudmen.cloudguard.unit.service.teamleader;

import com.cloudmen.cloudguard.service.teamleader.TeamleaderAccessService;
import com.cloudmen.cloudguard.service.teamleader.TeamleaderCompanyService;
import com.cloudmen.cloudguard.service.teamleader.TeamleaderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;

import java.nio.charset.StandardCharsets;

import static com.cloudmen.cloudguard.unit.helper.GlobalTestHelper.ADMIN;
import static com.cloudmen.cloudguard.unit.helper.TeamleaderTestHelper.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TeamleaderAccessServiceTest {

    @Mock
    private TeamleaderService teamleaderService;

    @Mock
    private TeamleaderCompanyService teamleaderCompanyService;

    private TeamleaderAccessService teamleaderAccessService;

    private static final String CLOUDGUARD_FIELD_ID = "mock-field-id-123";

    @BeforeEach
    void setUp() {
        teamleaderAccessService = new TeamleaderAccessService(teamleaderService, teamleaderCompanyService);
        ReflectionTestUtils.setField(teamleaderAccessService, "cloudGuardFieldId", CLOUDGUARD_FIELD_ID);
    }

    @Test
    void updateCredentials_delegatesToTeamleaderService() {
        teamleaderAccessService.updateCredentials("access", "refresh");
        verify(teamleaderService).updateCredentials("access", "refresh");
    }

    @Test
    void hasCloudGuardAccess_companyNotFound_returnsFalse() {
        HttpHeaders mockHeaders = new HttpHeaders();
        when(teamleaderService.createHeaders()).thenReturn(mockHeaders);
        when(teamleaderCompanyService.getCompanyIdByDomain(ADMIN, mockHeaders)).thenReturn(null);

        boolean result = teamleaderAccessService.hasCloudGuardAccess(ADMIN);

        assertFalse(result);
    }

    @Test
    void hasCloudGuardAccess_companyDetailsNull_returnsFalse() {
        HttpHeaders mockHeaders = new HttpHeaders();
        when(teamleaderService.createHeaders()).thenReturn(mockHeaders);
        when(teamleaderCompanyService.getCompanyIdByDomain(ADMIN, mockHeaders)).thenReturn("company-1");
        when(teamleaderCompanyService.getCompanyDetails("company-1", mockHeaders)).thenReturn(null);

        boolean result = teamleaderAccessService.hasCloudGuardAccess(ADMIN);

        assertFalse(result);
    }

    @Test
    void hasCloudGuardAccess_noCustomFields_returnsFalse() {
        HttpHeaders mockHeaders = new HttpHeaders();
        when(teamleaderService.createHeaders()).thenReturn(mockHeaders);
        when(teamleaderCompanyService.getCompanyIdByDomain(ADMIN, mockHeaders)).thenReturn("company-1");
        when(teamleaderCompanyService.getCompanyDetails("company-1", mockHeaders))
                .thenReturn(createCompanyDetailsWithoutCustomFields());

        boolean result = teamleaderAccessService.hasCloudGuardAccess(ADMIN);

        assertFalse(result);
    }

    @Test
    void hasCloudGuardAccess_customFieldFalse_returnsFalse() {
        HttpHeaders mockHeaders = new HttpHeaders();
        when(teamleaderService.createHeaders()).thenReturn(mockHeaders);
        when(teamleaderCompanyService.getCompanyIdByDomain(ADMIN, mockHeaders)).thenReturn("company-1");
        when(teamleaderCompanyService.getCompanyDetails("company-1", mockHeaders))
                .thenReturn(createCompanyDetailsWithCustomField(CLOUDGUARD_FIELD_ID, false));

        boolean result = teamleaderAccessService.hasCloudGuardAccess(ADMIN);

        assertFalse(result);
    }

    @Test
    void hasCloudGuardAccess_customFieldTrue_returnsTrue() {
        HttpHeaders mockHeaders = new HttpHeaders();
        when(teamleaderService.createHeaders()).thenReturn(mockHeaders);
        when(teamleaderCompanyService.getCompanyIdByDomain(ADMIN, mockHeaders)).thenReturn("company-1");
        when(teamleaderCompanyService.getCompanyDetails("company-1", mockHeaders))
                .thenReturn(createCompanyDetailsWithCustomField(CLOUDGUARD_FIELD_ID, true));

        boolean result = teamleaderAccessService.hasCloudGuardAccess(ADMIN);

        assertTrue(result);
    }

    @Test
    void hasCloudGuardAccess_differentFieldIdIsTrue_returnsFalse() {
        HttpHeaders mockHeaders = new HttpHeaders();
        when(teamleaderService.createHeaders()).thenReturn(mockHeaders);
        when(teamleaderCompanyService.getCompanyIdByDomain(ADMIN, mockHeaders)).thenReturn("company-1");
        when(teamleaderCompanyService.getCompanyDetails("company-1", mockHeaders))
                .thenReturn(createCompanyDetailsWithCustomField("wrong-field-id", true));

        boolean result = teamleaderAccessService.hasCloudGuardAccess(ADMIN);

        assertFalse(result);
    }

    @Test
    void hasCloudGuardAccess_unauthorizedAndRefreshSucceedsAndRetrySucceeds_returnsTrue() {
        HttpHeaders mockHeaders = new HttpHeaders();

        when(teamleaderService.createHeaders())
                .thenThrow(HttpClientErrorException.create(HttpStatus.UNAUTHORIZED, "Unauthorized", mockHeaders, new byte[0], StandardCharsets.UTF_8))
                .thenReturn(mockHeaders);

        when(teamleaderService.refreshTokens()).thenReturn(true);

        when(teamleaderCompanyService.getCompanyIdByDomain(ADMIN, mockHeaders)).thenReturn("company-1");
        when(teamleaderCompanyService.getCompanyDetails("company-1", mockHeaders))
                .thenReturn(createCompanyDetailsWithCustomField(CLOUDGUARD_FIELD_ID, true));

        boolean result = teamleaderAccessService.hasCloudGuardAccess(ADMIN);

        assertTrue(result);
        verify(teamleaderService, times(2)).createHeaders();
        verify(teamleaderService).refreshTokens();
    }

    @Test
    void hasCloudGuardAccess_unauthorizedAndRefreshSucceedsAndRetryFails_returnsFalse() {
        HttpHeaders mockHeaders = new HttpHeaders();

        when(teamleaderService.createHeaders())
                .thenThrow(HttpClientErrorException.create(HttpStatus.UNAUTHORIZED, "Unauthorized", mockHeaders, new byte[0], StandardCharsets.UTF_8))
                .thenReturn(mockHeaders);

        when(teamleaderService.refreshTokens()).thenReturn(true);
        when(teamleaderCompanyService.getCompanyIdByDomain(ADMIN, mockHeaders)).thenThrow(new RuntimeException("API Down"));

        boolean result = teamleaderAccessService.hasCloudGuardAccess(ADMIN);

        assertFalse(result);
        verify(teamleaderService, times(2)).createHeaders();
        verify(teamleaderService).refreshTokens();
    }

    @Test
    void hasCloudGuardAccess_unauthorizedAndRefreshFails_returnsFalse() {
        HttpHeaders mockHeaders = new HttpHeaders();

        when(teamleaderService.createHeaders())
                .thenThrow(HttpClientErrorException.create(HttpStatus.UNAUTHORIZED, "Unauthorized", mockHeaders, new byte[0], StandardCharsets.UTF_8));

        when(teamleaderService.refreshTokens()).thenReturn(false);

        boolean result = teamleaderAccessService.hasCloudGuardAccess(ADMIN);

        assertFalse(result);
        verify(teamleaderService, times(1)).createHeaders();
        verify(teamleaderService).refreshTokens();
    }

    @Test
    void hasCloudGuardAccess_genericException_returnsFalse() {
        when(teamleaderService.createHeaders()).thenThrow(new RuntimeException("Generic Error"));

        boolean result = teamleaderAccessService.hasCloudGuardAccess(ADMIN);

        assertFalse(result);
        verify(teamleaderService, never()).refreshTokens();
    }
}
