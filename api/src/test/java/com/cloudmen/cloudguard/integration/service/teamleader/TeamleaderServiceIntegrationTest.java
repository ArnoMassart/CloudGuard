package com.cloudmen.cloudguard.integration.service.teamleader;

import com.cloudmen.cloudguard.dto.TeamleaderTokens;
import com.cloudmen.cloudguard.exception.AccessTokenEmptyException;
import com.cloudmen.cloudguard.exception.RefreshTokenEmptyException;
import com.cloudmen.cloudguard.service.AdminSecurityKeysService;
import com.cloudmen.cloudguard.service.SupabaseTokenService;
import com.cloudmen.cloudguard.service.UserService;
import com.cloudmen.cloudguard.utility.GoogleApiFactory;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.test.context.TestPropertySource;

import com.cloudmen.cloudguard.service.teamleader.TeamleaderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = {TeamleaderService.class})
@TestPropertySource(properties = {
        "teamleader.client.id=test-client-id",
        "teamleader.client.secret=test-client-secret"
})
public class TeamleaderServiceIntegrationTest {

    @Autowired
    private TeamleaderService teamleaderService;

    @MockitoBean
    private SupabaseTokenService supabaseTokenService;

    @MockitoBean
    private GoogleApiFactory googleApiFactory;

    @MockitoBean
    private AdminSecurityKeysService adminSecurityKeysService;

    @Mock
    private RestTemplate restTemplate;

    private static final String ACCESS_TOKEN = "valid-access-token";
    private static final String REFRESH_TOKEN = "valid-refresh-token";

    @BeforeEach
    void setUp(){
        MockitoAnnotations.openMocks(this);
        ReflectionTestUtils.setField(teamleaderService, "restTemplate", restTemplate);
    }

    @Test
    void updateCredentials_validTokens_updatesSupabase() {
        teamleaderService.updateCredentials(ACCESS_TOKEN, REFRESH_TOKEN);

        verify(supabaseTokenService).updateTokens(ACCESS_TOKEN, REFRESH_TOKEN);
    }

    @Test
    void updateCredentials_emptyAccessToken_throwsException() {
        assertThrows(AccessTokenEmptyException.class, () -> {
            teamleaderService.updateCredentials("", REFRESH_TOKEN);
        });

        assertThrows(AccessTokenEmptyException.class, () -> {
            teamleaderService.updateCredentials(null, REFRESH_TOKEN);
        });

        verify(supabaseTokenService, never()).updateTokens(anyString(), anyString());
    }

    @Test
    void updateCredentials_emptyRefreshToken_throwsException() {
        assertThrows(RefreshTokenEmptyException.class, () -> {
            teamleaderService.updateCredentials(ACCESS_TOKEN, "");
        });

        assertThrows(RefreshTokenEmptyException.class, () -> {
            teamleaderService.updateCredentials(ACCESS_TOKEN, null);
        });

        verify(supabaseTokenService, never()).updateTokens(anyString(), anyString());
    }

    @Test
    void createHeaders_returnsHeadersWithBearerToken() {
        when(supabaseTokenService.getTokens()).thenReturn(new TeamleaderTokens(ACCESS_TOKEN, REFRESH_TOKEN));

        HttpHeaders headers = teamleaderService.createHeaders();

        assertNotNull(headers);
        assertEquals("Bearer " + ACCESS_TOKEN, headers.getFirst(HttpHeaders.AUTHORIZATION));
        assertEquals(MediaType.APPLICATION_JSON, headers.getContentType());
        verify(supabaseTokenService, times(1)).getTokens();
    }

    @Test
    void refreshTokens_success_updatesSupabaseAndReturnsTrue() {
        when(supabaseTokenService.getTokens()).thenReturn(new TeamleaderTokens(ACCESS_TOKEN, REFRESH_TOKEN));

        Map<String, Object> responseBody = Map.of(
                "access_token", "new-access-token",
                "refresh_token", "new-refresh-token"
        );
        ResponseEntity<Map<String, Object>> responseEntity = new ResponseEntity<>(responseBody, HttpStatus.OK);

        when(restTemplate.exchange(
                eq("https://focus.teamleader.eu/oauth2/access_token"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenReturn(responseEntity);

        boolean result = teamleaderService.refreshTokens();

        assertTrue(result);
        verify(supabaseTokenService).updateTokens("new-access-token", "new-refresh-token");
    }

    @Test
    void refreshTokens_emptyBody_returnsFalse() {
        when(supabaseTokenService.getTokens()).thenReturn(new TeamleaderTokens(ACCESS_TOKEN, REFRESH_TOKEN));

        ResponseEntity<Map<String, Object>> responseEntity = new ResponseEntity<>(null, HttpStatus.OK);

        when(restTemplate.exchange(
                eq("https://focus.teamleader.eu/oauth2/access_token"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenReturn(responseEntity);

        boolean result = teamleaderService.refreshTokens();

        assertFalse(result);
        verify(supabaseTokenService, never()).updateTokens(anyString(), anyString());
    }

    @Test
    void refreshTokens_apiError_returnsFalse() {
        when(supabaseTokenService.getTokens()).thenReturn(new TeamleaderTokens(ACCESS_TOKEN, REFRESH_TOKEN));

        when(restTemplate.exchange(
                eq("https://focus.teamleader.eu/oauth2/access_token"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenThrow(new HttpClientErrorException(HttpStatus.UNAUTHORIZED));

        boolean result = teamleaderService.refreshTokens();

        assertFalse(result);
        verify(supabaseTokenService, never()).updateTokens(anyString(), anyString());
    }
}
