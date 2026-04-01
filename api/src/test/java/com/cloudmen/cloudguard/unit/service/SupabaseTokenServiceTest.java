package com.cloudmen.cloudguard.unit.service;

import com.cloudmen.cloudguard.dto.TeamleaderTokens;
import com.cloudmen.cloudguard.exception.TokensNotFoundException;
import com.cloudmen.cloudguard.service.SupabaseTokenService;
import com.cloudmen.cloudguard.unit.helper.SupabaseTokenTestHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
public class SupabaseTokenServiceTest {

    @Mock
    private RestTemplate restTemplate;

    private SupabaseTokenService service;

    private static final String MOCK_URL = "https://mock.supabase.co";
    private static final String MOCK_KEY = "mock-api-key";

    @BeforeEach
    void setUp() {
        service = new SupabaseTokenService(MOCK_URL, MOCK_KEY);
        ReflectionTestUtils.setField(service, "restTemplate", restTemplate);
    }

    @Test
    void getTokens_validResponse_returnsTokens() {
        ResponseEntity<List<Map<String, String>>> mockResponse = SupabaseTokenTestHelper.mockValidTokenResponse("access-123", "refresh-456");
        when(restTemplate.exchange(
                eq(MOCK_URL + "/rest/v1/teamleader_tokens?id=eq.1"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenReturn(mockResponse);

        TeamleaderTokens result = service.getTokens();

        assertNotNull(result);
        assertEquals("access-123", result.accessToken());
        assertEquals("refresh-456", result.refreshToken());
    }

    @Test
    void getTokens_emptyListResponse_throwsTokensNotFoundException() {
        ResponseEntity<List<Map<String, String>>> mockResponse = SupabaseTokenTestHelper.mockEmptyResponse();
        when(restTemplate.exchange(
                anyString(), eq(HttpMethod.GET),any(HttpEntity.class), any(ParameterizedTypeReference.class)
        )).thenReturn(mockResponse);

        TokensNotFoundException exception = assertThrows(TokensNotFoundException.class, () -> service.getTokens());
        assertTrue(exception.getMessage().contains("Geen Teamleader tokens gevonden in Supabase"));
    }

    @Test
    void getTokens_nullBodyResponse_throwsTokensNotFoundException() {
        ResponseEntity<List<Map<String, String>>> mockResponse = SupabaseTokenTestHelper.mockNullBodyResponse();
        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenReturn(mockResponse);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> service.getTokens());
        assertTrue(exception.getMessage().contains("Geen Teamleader tokens gevonden in Supabase"));
    }

    @Test
    void updateTokens_sendsPatchRequestWithCorrectDataAndHeaders() {
        service.updateTokens("new-access", "new-refresh");

        ArgumentCaptor<HttpEntity<Map<String, String>>> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);

        verify(restTemplate).exchange(
                eq(MOCK_URL + "/rest/v1/teamleader_tokens?id=eq.1"),
                eq(HttpMethod.PATCH),
                entityCaptor.capture(),
                eq(String.class)
        );

        HttpEntity<Map<String, String>> capturedEntity = entityCaptor.getValue();
        Map<String, String> body = capturedEntity.getBody();

        assertNotNull(body);
        assertEquals("new-access", body.get("access_token"));
        assertEquals("new-refresh", body.get("refresh_token"));

        assertEquals(MOCK_KEY, capturedEntity.getHeaders().getFirst("apikey"));
        assertEquals("Bearer " + MOCK_KEY, capturedEntity.getHeaders().getFirst("Authorization"));
        assertEquals("application/json", capturedEntity.getHeaders().getFirst("Content-Type"));
        assertEquals("return=representation", capturedEntity.getHeaders().getFirst("Prefer"));
    }
}
