package com.cloudmen.cloudguard.unit.service.teamleader;

import com.cloudmen.cloudguard.dto.TeamleaderTokens;
import com.cloudmen.cloudguard.exception.AccessTokenEmptyException;
import com.cloudmen.cloudguard.exception.RefreshTokenEmptyException;
import com.cloudmen.cloudguard.service.SupabaseTokenService;
import com.cloudmen.cloudguard.service.teamleader.TeamleaderService;
import com.cloudmen.cloudguard.unit.helper.TeamleaderTestHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
public class TeamleaderServiceTest {

    @Mock
    private SupabaseTokenService supabaseTokenService;

    @Mock
    private RestTemplate restTemplate;

    private TeamleaderService service;

    private static final String CLIENT_ID = "mock-client-id";
    private static final String CLIENT_SECRET = "mock-client-secret";

    @BeforeEach
    void setUp() {
        service = new TeamleaderService(supabaseTokenService);
        ReflectionTestUtils.setField(service, "restTemplate", restTemplate);
        ReflectionTestUtils.setField(service, "clientId", CLIENT_ID);
        ReflectionTestUtils.setField(service, "clientSecret", CLIENT_SECRET);
    }

    @Test
    void updateCredentials_validTokens_updatesSupabase() {
        service.updateCredentials("valid-access", "valid-refresh");

        verify(supabaseTokenService).updateTokens("valid-access", "valid-refresh");
    }

    @Test
    void updateCredentials_nullAccessToken_throwsException() {
        assertThrows(AccessTokenEmptyException.class, () -> service.updateCredentials(null, "refresh"));
    }

    @Test
    void updateCredentials_blankAccessToken_throwsException() {
        assertThrows(AccessTokenEmptyException.class, () -> service.updateCredentials("   ", "refresh"));
    }

    @Test
    void updateCredentials_nullRefreshToken_throwsException() {
        assertThrows(RefreshTokenEmptyException.class, () -> service.updateCredentials("access", null));
    }

    @Test
    void updateCredentials_blankRefreshToken_throwsException() {
        assertThrows(RefreshTokenEmptyException.class, () -> service.updateCredentials("access", "  "));
    }

    @Test
    void createHeaders_returnsCorrectHeaders() {
        TeamleaderTokens tokens = TeamleaderTestHelper.createMockTokens("access-123", "refresh-456");
        when(supabaseTokenService.getTokens()).thenReturn(tokens);

        HttpHeaders headers = service.createHeaders();

        assertEquals("Bearer access-123", headers.getFirst(HttpHeaders.AUTHORIZATION));
        assertEquals(MediaType.APPLICATION_JSON, headers.getContentType());
    }

    @Test
    @SuppressWarnings("unchecked")
    void refreshTokens_success_returnsTrueAndUpdatesSupabase() {
        TeamleaderTokens oldTokens = TeamleaderTestHelper.createMockTokens("old-access", "old-refresh");
        when(supabaseTokenService.getTokens()).thenReturn(oldTokens);

        ResponseEntity<Map<String, Object>> mockResponse = TeamleaderTestHelper.createMockOAuthResponse("new-access", "new-refresh");

        ArgumentCaptor<HttpEntity<MultiValueMap<String, String>>> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);

        when(restTemplate.exchange(
                eq("https://focus.teamleader.eu/oauth2/access_token"),
                eq(HttpMethod.POST),
                entityCaptor.capture(),
                any(ParameterizedTypeReference.class)
        )).thenReturn(mockResponse);

        boolean result = service.refreshTokens();

        assertTrue(result);
        verify(supabaseTokenService).updateTokens("new-access", "new-refresh");

        HttpEntity<MultiValueMap<String, String>> capturedEntity = entityCaptor.getValue();
        MultiValueMap<String, String> body = capturedEntity.getBody();

        assertNotNull(body);
        assertEquals(CLIENT_ID, body.getFirst("client_id"));
        assertEquals(CLIENT_SECRET, body.getFirst("client_secret"));
        assertEquals("old-refresh", body.getFirst("refresh_token"));
        assertEquals("refresh_token", body.getFirst("grant_type"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void refreshTokens_restClientException_returnsFalse() {
        TeamleaderTokens oldTokens = TeamleaderTestHelper.createMockTokens("old-access", "old-refresh");
        when(supabaseTokenService.getTokens()).thenReturn(oldTokens);

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenThrow(new RestClientException("Teamleader API offline"));

        boolean result = service.refreshTokens();

        assertFalse(result);
        verify(supabaseTokenService, never()).updateTokens(anyString(), anyString());
    }

    @Test
    void refreshTokens_nullBody_returnsFalse() {
        TeamleaderTokens oldTokens = TeamleaderTestHelper.createMockTokens("old-access", "old-refresh");
        when(supabaseTokenService.getTokens()).thenReturn(oldTokens);

        ResponseEntity<Map<String, Object>> mockResponse = new ResponseEntity<>(null, HttpStatus.OK);

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenReturn(mockResponse);

        boolean result = service.refreshTokens();

        assertFalse(result);
        verify(supabaseTokenService, never()).updateTokens(anyString(), anyString());
    }
}
