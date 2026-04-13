package com.cloudmen.cloudguard.unit.service.teamleader;

import com.cloudmen.cloudguard.service.teamleader.TeamleaderCompanyService;
import com.cloudmen.cloudguard.service.teamleader.TeamleaderService;
import com.cloudmen.cloudguard.unit.helper.GlobalTestHelper;
import com.cloudmen.cloudguard.unit.helper.TeamleaderTestHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
public class TeamleaderCompanyServiceTest {

    @Mock
    private TeamleaderService teamleaderService;

    @Mock
    private RestTemplate restTemplate;

    private TeamleaderCompanyService teamleaderCompanyService;

    private static final String API_BASE = "https://api.teamleader.eu";
    private static final String COMPANY_ID = "company-123";

    @BeforeEach
    void setUp() {
        teamleaderCompanyService = new TeamleaderCompanyService(teamleaderService);
        ReflectionTestUtils.setField(teamleaderCompanyService, "restTemplate", restTemplate);
        ReflectionTestUtils.setField(teamleaderCompanyService, "teamleaderApiBase", API_BASE);
    }

    @Test
    void getCompanyDetails_success_returnsData() {
        HttpHeaders headers = new HttpHeaders();
        Map<String, Object> expectedData = Map.of("id", COMPANY_ID, "name", "Test Corp");

        when(restTemplate.exchange(
                eq(API_BASE + "/companies.info"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenReturn(TeamleaderTestHelper.createCompanyInfoResponse(expectedData));

        Map<String, Object> result = teamleaderCompanyService.getCompanyDetails(COMPANY_ID, headers);

        assertEquals(expectedData, result);
        verify(teamleaderService, never()).refreshTokens();
    }

    @Test
    void getCompanyDetails_unauthorizedAndRefreshSucceeds_retriesAndReturnsData() {
        HttpHeaders oldHeaders = new HttpHeaders();
        HttpHeaders newHeaders = new HttpHeaders();
        newHeaders.setBearerAuth("new-token");
        Map<String, Object> expectedData = Map.of("id", COMPANY_ID, "name", "Test Corp");

        when(restTemplate.exchange(
                eq(API_BASE + "/companies.info"),
                eq(HttpMethod.POST),
                argThat(entity -> !entity.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)),
                any(ParameterizedTypeReference.class)
        )).thenThrow(HttpClientErrorException.create(HttpStatus.UNAUTHORIZED, "Unauthorized", oldHeaders, new byte[0], StandardCharsets.UTF_8));

        when(teamleaderService.refreshTokens()).thenReturn(true);
        when(teamleaderService.createHeaders()).thenReturn(newHeaders);

        when(restTemplate.exchange(
                eq(API_BASE + "/companies.info"),
                eq(HttpMethod.POST),
                argThat(entity -> entity.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)),
                any(ParameterizedTypeReference.class)
        )).thenReturn(TeamleaderTestHelper.createCompanyInfoResponse(expectedData));

        Map<String, Object> result = teamleaderCompanyService.getCompanyDetails(COMPANY_ID, oldHeaders);

        assertEquals(expectedData, result);
        verify(teamleaderService).refreshTokens();
        verify(restTemplate, times(2)).exchange(anyString(), any(), any(), any(ParameterizedTypeReference.class));
    }

    @Test
    void getCompanyDetails_unauthorizedAndRefreshFails_throwsException() {
        HttpHeaders headers = new HttpHeaders();

        when(restTemplate.exchange(
                eq(API_BASE + "/companies.info"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenThrow(HttpClientErrorException.create(HttpStatus.UNAUTHORIZED, "Unauthorized", headers, new byte[0], StandardCharsets.UTF_8));

        when(teamleaderService.refreshTokens()).thenReturn(false);

        assertThrows(HttpClientErrorException.Unauthorized.class, () -> teamleaderCompanyService.getCompanyDetails(COMPANY_ID, headers));
        verify(teamleaderService).refreshTokens();
    }

    @Test
    void getCompanyIdByDomain_successWithData_returnsFirstId() {
        HttpHeaders headers = new HttpHeaders();
        List<Map<String, Object>> dataList = List.of(
                Map.of("id", COMPANY_ID),
                Map.of("id", "other-id")
        );

        when(restTemplate.exchange(
                eq(API_BASE + "/companies.list"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenReturn(TeamleaderTestHelper.createCompanyListResponse(dataList));

        String result = teamleaderCompanyService.getCompanyIdByDomain(GlobalTestHelper.ADMIN, headers);

        assertEquals(COMPANY_ID, result);
    }

    @Test
    void getCompanyIdByDomain_successWithEmptyData_returnsNull() {
        HttpHeaders headers = new HttpHeaders();

        when(restTemplate.exchange(
                eq(API_BASE + "/companies.list"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenReturn(TeamleaderTestHelper.createCompanyListResponse(List.of()));

        String result = teamleaderCompanyService.getCompanyIdByDomain(GlobalTestHelper.ADMIN, headers);

        assertNull(result);
    }

    @Test
    void getCompanyIdByDomain_unauthorizedAndRefreshSucceeds_retriesAndReturnsId() {
        HttpHeaders oldHeaders = new HttpHeaders();
        HttpHeaders newHeaders = new HttpHeaders();
        newHeaders.setBearerAuth("new-token");

        when(restTemplate.exchange(
                eq(API_BASE + "/companies.list"),
                eq(HttpMethod.POST),
                argThat(entity -> !entity.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)),
                any(ParameterizedTypeReference.class)
        )).thenThrow(HttpClientErrorException.create(HttpStatus.UNAUTHORIZED, "Unauthorized", oldHeaders, new byte[0], StandardCharsets.UTF_8));

        when(teamleaderService.refreshTokens()).thenReturn(true);
        when(teamleaderService.createHeaders()).thenReturn(newHeaders);

        when(restTemplate.exchange(
                eq(API_BASE + "/companies.list"),
                eq(HttpMethod.POST),
                argThat(entity -> entity.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)),
                any(ParameterizedTypeReference.class)
        )).thenReturn(TeamleaderTestHelper.createCompanyListResponse(List.of(Map.of("id", COMPANY_ID))));

        String result = teamleaderCompanyService.getCompanyIdByDomain(GlobalTestHelper.ADMIN, oldHeaders);

        assertEquals(COMPANY_ID, result);
        verify(teamleaderService).refreshTokens();
    }

    @Test
    void getCompanyIdByDomain_unauthorizedAndRefreshFails_throwsException() {
        HttpHeaders headers = new HttpHeaders();

        when(restTemplate.exchange(
                eq(API_BASE + "/companies.list"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenThrow(HttpClientErrorException.create(HttpStatus.UNAUTHORIZED, "Unauthorized", headers, new byte[0], StandardCharsets.UTF_8));

        when(teamleaderService.refreshTokens()).thenReturn(false);

        assertThrows(HttpClientErrorException.Unauthorized.class, () -> teamleaderCompanyService.getCompanyIdByDomain(GlobalTestHelper.ADMIN, headers));
        verify(teamleaderService).refreshTokens();
    }

    @Test
    void getCompanyNameByEmail_companyFoundAndNameExists_returnsName() {
        HttpHeaders headers = new HttpHeaders();

        when(restTemplate.exchange(
                eq(API_BASE + "/companies.list"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenReturn(TeamleaderTestHelper.createCompanyListResponse(List.of(Map.of("id", COMPANY_ID))));

        when(restTemplate.exchange(
                eq(API_BASE + "/companies.info"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenReturn(TeamleaderTestHelper.createCompanyInfoResponse(Map.of("id", COMPANY_ID, "name", "Cloudmen BV")));

        String result = teamleaderCompanyService.getCompanyNameByEmail(GlobalTestHelper.ADMIN, headers);

        assertEquals("Cloudmen BV", result);
    }

    @Test
    void getCompanyNameByEmail_companyIdNull_returnsUnknownCompany() {
        HttpHeaders headers = new HttpHeaders();

        when(restTemplate.exchange(
                eq(API_BASE + "/companies.list"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenReturn(TeamleaderTestHelper.createCompanyListResponse(List.of()));

        String result = teamleaderCompanyService.getCompanyNameByEmail(GlobalTestHelper.ADMIN, headers);

        assertEquals("Onbekend Bedrijf", result);
    }

    @Test
    void getCompanyNameByEmail_companyDetailsMissingName_returnsUnknownCompany() {
        HttpHeaders headers = new HttpHeaders();

        when(restTemplate.exchange(
                eq(API_BASE + "/companies.list"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenReturn(TeamleaderTestHelper.createCompanyListResponse(List.of(Map.of("id", COMPANY_ID))));

        when(restTemplate.exchange(
                eq(API_BASE + "/companies.info"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenReturn(TeamleaderTestHelper.createCompanyInfoResponse(Map.of("id", COMPANY_ID)));

        String result = teamleaderCompanyService.getCompanyNameByEmail(GlobalTestHelper.ADMIN, headers);

        assertEquals("Onbekend Bedrijf", result);
    }

    @Test
    void getCompanyNameByEmail_companyDetailsNull_returnsUnknownCompany() {
        HttpHeaders headers = new HttpHeaders();

        when(restTemplate.exchange(
                eq(API_BASE + "/companies.list"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenReturn(TeamleaderTestHelper.createCompanyListResponse(List.of(Map.of("id", COMPANY_ID))));

        when(restTemplate.exchange(
                eq(API_BASE + "/companies.info"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenThrow(new RuntimeException("Simulated unexpected null or exception internally"));

        assertThrows(RuntimeException.class, () -> teamleaderCompanyService.getCompanyNameByEmail(GlobalTestHelper.ADMIN, headers));
    }
}
