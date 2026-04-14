package com.cloudmen.cloudguard.integration.service.teamleader;

import com.cloudmen.cloudguard.service.AdminSecurityKeysService;
import com.cloudmen.cloudguard.service.teamleader.TeamleaderAccessService;
import com.cloudmen.cloudguard.service.teamleader.TeamleaderCompanyService;
import com.cloudmen.cloudguard.service.teamleader.TeamleaderService;
import com.cloudmen.cloudguard.utility.GoogleApiFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.HttpClientErrorException;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = {TeamleaderAccessService.class})
@TestPropertySource(properties = {
        "teamleader.customfield.cloudguard.id=custom-field-123"
})
public class TeamleaderAccessServiceIT {
    @Autowired
    private TeamleaderAccessService teamleaderAccessService;

    @MockitoBean
    private TeamleaderService teamleaderService;

    @MockitoBean
    private TeamleaderCompanyService teamleaderCompanyService;

    @MockitoBean
    private GoogleApiFactory googleApiFactory;

    @MockitoBean
    private AdminSecurityKeysService adminSecurityKeysService;

    private HttpHeaders mockHeaders;
    private static final String EMAIL = "admin@cloudmen.com";
    private static final String COMPANY_ID = "company-456";

    @BeforeEach
    void setUp() {
        mockHeaders = new HttpHeaders();
        when(teamleaderService.createHeaders()).thenReturn(mockHeaders);
    }

    @Test
    void hasCloudGuardAccess_success_returnsTrue() {
        when(teamleaderCompanyService.getCompanyIdByDomain(EMAIL, mockHeaders)).thenReturn(COMPANY_ID);

        Map<String, Object> customField = Map.of(
                "definition", Map.of("id", "custom-field-123"),
                "value", true
        );
        Map<String, Object> companyDetails = Map.of("custom_fields", List.of(customField));

        when(teamleaderCompanyService.getCompanyDetails(COMPANY_ID, mockHeaders)).thenReturn(companyDetails);

        assertTrue(teamleaderAccessService.hasCloudGuardAccess(EMAIL));
    }

    @Test
    void hasCloudGuardAccess_customFieldFalse_returnsFalse() {
        when(teamleaderCompanyService.getCompanyIdByDomain(EMAIL, mockHeaders)).thenReturn(COMPANY_ID);

        Map<String, Object> customField = Map.of(
                "definition", Map.of("id", "custom-field-123"),
                "value", false
        );
        Map<String, Object> companyDetails = Map.of("custom_fields", List.of(customField));

        when(teamleaderCompanyService.getCompanyDetails(COMPANY_ID, mockHeaders)).thenReturn(companyDetails);

        assertFalse(teamleaderAccessService.hasCloudGuardAccess(EMAIL));
    }

    @Test
    void hasCloudGuardAccess_noCompanyFound_returnsFalse() {
        when(teamleaderCompanyService.getCompanyIdByDomain(EMAIL, mockHeaders)).thenReturn(null);

        assertFalse(teamleaderAccessService.hasCloudGuardAccess(EMAIL));
    }

    @Test
    void hasCloudGuardAccess_unauthorizedAndRefreshSuccess_retriesAndReturnsTrue() {
        HttpClientErrorException.Unauthorized unauthorizedException = (HttpClientErrorException.Unauthorized) HttpClientErrorException.create(
                HttpStatus.UNAUTHORIZED, "Unauthorized", HttpHeaders.EMPTY, new byte[0], StandardCharsets.UTF_8
        );

        when(teamleaderCompanyService.getCompanyIdByDomain(EMAIL, mockHeaders))
                .thenThrow(unauthorizedException)
                .thenReturn(COMPANY_ID);

        when(teamleaderService.refreshTokens()).thenReturn(true);

        Map<String, Object> customField = Map.of(
                "definition", Map.of("id", "custom-field-123"),
                "value", true
        );
        Map<String, Object> companyDetails = Map.of("custom_fields", List.of(customField));
        when(teamleaderCompanyService.getCompanyDetails(COMPANY_ID, mockHeaders)).thenReturn(companyDetails);

        assertTrue(teamleaderAccessService.hasCloudGuardAccess(EMAIL));

        verify(teamleaderService).refreshTokens();
        verify(teamleaderCompanyService, times(2)).getCompanyIdByDomain(EMAIL, mockHeaders);
    }

    @Test
    void hasCloudGuardAccess_unauthorizedAndRefreshFails_returnsFalse() {
        HttpClientErrorException.Unauthorized unauthorizedException = (HttpClientErrorException.Unauthorized) HttpClientErrorException.create(
                HttpStatus.UNAUTHORIZED, "Unauthorized", HttpHeaders.EMPTY, new byte[0], StandardCharsets.UTF_8
        );

        when(teamleaderCompanyService.getCompanyIdByDomain(EMAIL, mockHeaders))
                .thenThrow(unauthorizedException);

        when(teamleaderService.refreshTokens()).thenReturn(false);

        assertFalse(teamleaderAccessService.hasCloudGuardAccess(EMAIL));

        verify(teamleaderService).refreshTokens();
        verify(teamleaderCompanyService, times(1)).getCompanyIdByDomain(EMAIL, mockHeaders);
    }

    @Test
    void hasCloudGuardAccess_genericException_returnsFalse() {
        when(teamleaderCompanyService.getCompanyIdByDomain(EMAIL, mockHeaders))
                .thenThrow(new RuntimeException("Unexpected Error"));

        assertFalse(teamleaderAccessService.hasCloudGuardAccess(EMAIL));
    }
}
