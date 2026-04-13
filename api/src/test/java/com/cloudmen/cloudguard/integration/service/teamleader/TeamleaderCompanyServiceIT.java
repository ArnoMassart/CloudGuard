package com.cloudmen.cloudguard.integration.service.teamleader;

import com.cloudmen.cloudguard.service.AdminSecurityKeysService;
import com.cloudmen.cloudguard.service.teamleader.TeamleaderCompanyService;
import com.cloudmen.cloudguard.service.teamleader.TeamleaderService;
import com.cloudmen.cloudguard.utility.GoogleApiFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = {TeamleaderCompanyService.class})
@TestPropertySource(properties = {
        "teamleader.api.base=https://api.teamleader.eu"
})
public class TeamleaderCompanyServiceIT {
    @Autowired
    private TeamleaderCompanyService teamleaderCompanyService;

    @MockitoBean
    private TeamleaderService teamleaderService;

    @MockitoBean
    private GoogleApiFactory googleApiFactory;

    @MockitoBean
    private AdminSecurityKeysService adminSecurityKeysService;

    @Mock
    private RestTemplate restTemplate;

    private HttpHeaders mockHeaders;
    private static final String COMPANY_ID = "comp-123";
    private static final String EMAIL = "user@cloudmen.com";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        ReflectionTestUtils.setField(teamleaderCompanyService, "restTemplate", restTemplate);

        mockHeaders = new HttpHeaders();
        when(teamleaderService.createHeaders()).thenReturn(mockHeaders);
    }

    @Test
    void getCompanyDetails_success_returnsCompanyDetails() {
        Map<String, Object> expectedData = Map.of("id", COMPANY_ID, "name", "Cloudmen BV");
        ResponseEntity<Map<String, Object>> response = new ResponseEntity<>(Map.of("data", expectedData), HttpStatus.OK);

        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), any(ParameterizedTypeReference.class)))
                .thenReturn(response);

        Map<String, Object> result = teamleaderCompanyService.getCompanyDetails(COMPANY_ID, mockHeaders);

        assertEquals(expectedData, result);
        verify(restTemplate, times(1)).exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), any(ParameterizedTypeReference.class));
    }

    @Test
    void getCompanyDetails_unauthorizedAndRefreshSuccess_retriesAndReturnsDetails() {
        Map<String, Object> expectedData = Map.of("id", COMPANY_ID, "name", "Cloudmen BV");
        ResponseEntity<Map<String, Object>> successResponse = new ResponseEntity<>(Map.of("data", expectedData), HttpStatus.OK);

        HttpClientErrorException.Unauthorized unauthorizedException = (HttpClientErrorException.Unauthorized) HttpClientErrorException.create(
                HttpStatus.UNAUTHORIZED, "Unauthorized", HttpHeaders.EMPTY, new byte[0], StandardCharsets.UTF_8
        );

        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), any(ParameterizedTypeReference.class)))
                .thenThrow(unauthorizedException)
                .thenReturn(successResponse);

        when(teamleaderService.refreshTokens()).thenReturn(true);

        Map<String, Object> result = teamleaderCompanyService.getCompanyDetails(COMPANY_ID, mockHeaders);

        assertEquals(expectedData, result);
        verify(teamleaderService, times(1)).refreshTokens();
        verify(restTemplate, times(2)).exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), any(ParameterizedTypeReference.class));
    }

    @Test
    void getCompanyDetails_unauthorizedAndRefreshFails_throwsException() {
        HttpClientErrorException.Unauthorized unauthorizedException = (HttpClientErrorException.Unauthorized) HttpClientErrorException.create(
                HttpStatus.UNAUTHORIZED, "Unauthorized", HttpHeaders.EMPTY, new byte[0], StandardCharsets.UTF_8
        );

        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), any(ParameterizedTypeReference.class)))
                .thenThrow(unauthorizedException);

        when(teamleaderService.refreshTokens()).thenReturn(false);

        assertThrows(HttpClientErrorException.Unauthorized.class, () -> {
            teamleaderCompanyService.getCompanyDetails(COMPANY_ID, mockHeaders);
        });

        verify(teamleaderService, times(1)).refreshTokens();
        verify(restTemplate, times(1)).exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), any(ParameterizedTypeReference.class));
    }

    @Test
    void getCompanyIdByEmail_success_returnsCompanyId() {
        List<Map<String, Object>> dataList = List.of(Map.of("id", COMPANY_ID));
        ResponseEntity<Map<String, Object>> response = new ResponseEntity<>(Map.of("data", dataList), HttpStatus.OK);

        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), any(ParameterizedTypeReference.class)))
                .thenReturn(response);

        String result = teamleaderCompanyService.getCompanyIdByEmail(EMAIL, mockHeaders);

        assertEquals(COMPANY_ID, result);
    }

    @Test
    void getCompanyIdByEmail_emptyData_returnsNull() {
        ResponseEntity<Map<String, Object>> response = new ResponseEntity<>(Map.of("data", Collections.emptyList()), HttpStatus.OK);

        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), any(ParameterizedTypeReference.class)))
                .thenReturn(response);

        String result = teamleaderCompanyService.getCompanyIdByEmail(EMAIL, mockHeaders);

        assertNull(result);
    }

    @Test
    void getCompanyIdByEmail_unauthorizedAndRefreshSuccess_retriesAndReturnsId() {
        List<Map<String, Object>> dataList = List.of(Map.of("id", COMPANY_ID));
        ResponseEntity<Map<String, Object>> successResponse = new ResponseEntity<>(Map.of("data", dataList), HttpStatus.OK);

        HttpClientErrorException.Unauthorized unauthorizedException = (HttpClientErrorException.Unauthorized) HttpClientErrorException.create(
                HttpStatus.UNAUTHORIZED, "Unauthorized", HttpHeaders.EMPTY, new byte[0], StandardCharsets.UTF_8
        );

        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), any(ParameterizedTypeReference.class)))
                .thenThrow(unauthorizedException)
                .thenReturn(successResponse);

        when(teamleaderService.refreshTokens()).thenReturn(true);

        String result = teamleaderCompanyService.getCompanyIdByEmail(EMAIL, mockHeaders);

        assertEquals(COMPANY_ID, result);
        verify(teamleaderService, times(1)).refreshTokens();
        verify(restTemplate, times(2)).exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), any(ParameterizedTypeReference.class));
    }

    @Test
    void getCompanyNameByEmail_success_returnsCompanyName() {
        List<Map<String, Object>> listData = List.of(Map.of("id", COMPANY_ID));
        ResponseEntity<Map<String, Object>> listResponse = new ResponseEntity<>(Map.of("data", listData), HttpStatus.OK);

        Map<String, Object> infoData = Map.of("id", COMPANY_ID, "name", "Cloudmen BV");
        ResponseEntity<Map<String, Object>> infoResponse = new ResponseEntity<>(Map.of("data", infoData), HttpStatus.OK);

        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), any(ParameterizedTypeReference.class)))
                .thenReturn(listResponse)
                .thenReturn(infoResponse);

        String result = teamleaderCompanyService.getCompanyNameByEmail(EMAIL, mockHeaders);

        assertEquals("Cloudmen BV", result);
    }

    @Test
    void getCompanyNameByEmail_noCompanyId_returnsUnknown() {
        ResponseEntity<Map<String, Object>> listResponse = new ResponseEntity<>(Map.of("data", Collections.emptyList()), HttpStatus.OK);
        when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), any(ParameterizedTypeReference.class)))
                .thenReturn(listResponse);

        String result = teamleaderCompanyService.getCompanyNameByEmail(EMAIL, mockHeaders);

        assertEquals("Onbekend Bedrijf", result);
        verify(restTemplate, times(1)).exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), any(ParameterizedTypeReference.class));
    }
}
