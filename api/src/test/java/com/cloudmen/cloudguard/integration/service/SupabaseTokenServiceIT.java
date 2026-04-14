package com.cloudmen.cloudguard.integration.service;

import com.cloudmen.cloudguard.dto.TeamleaderTokens;
import com.cloudmen.cloudguard.exception.GoogleWorkspaceSyncException;
import com.cloudmen.cloudguard.service.AdminSecurityKeysService;
import com.cloudmen.cloudguard.service.SupabaseTokenService;
import com.cloudmen.cloudguard.utility.GoogleApiFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.MessageSource;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = {SupabaseTokenService.class})
@TestPropertySource(properties = {
        "supabase.url=https://mock.supabase.co",
        "supabase.key=mock-key"
})
public class SupabaseTokenServiceIT {

    @Autowired
    private SupabaseTokenService supabaseTokenService;

    @MockitoBean(name = "messageSource")
    private MessageSource messageSource;

    @MockitoBean
    private GoogleApiFactory googleApiFactory;

    @MockitoBean
    private AdminSecurityKeysService adminSecurityKeysService;

    @Mock
    private RestTemplate restTemplate;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        ReflectionTestUtils.setField(supabaseTokenService, "restTemplate", restTemplate);

        when(messageSource.getMessage(anyString(), any(), any(Locale.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void getTokens_success_returnsTeamleaderTokens() {
        Map<String, String> row = Map.of("access_token", "access-token-123", "refresh_token", "refresh-token-456");
        ResponseEntity<List<Map<String, String>>> response = new ResponseEntity<>(List.of(row), HttpStatus.OK);

        when(restTemplate.exchange(
                eq("https://mock.supabase.co/rest/v1/teamleader_tokens?id=eq.1"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenReturn(response);

        TeamleaderTokens tokens = supabaseTokenService.getTokens();

        assertNotNull(tokens);
        assertEquals("access-token-123", tokens.accessToken());
        assertEquals("refresh-token-456", tokens.refreshToken());
    }

    @Test
    void getTokens_emptyBody_throwsGoogleWorkspaceSyncException() {
        ResponseEntity<List<Map<String, String>>> response = new ResponseEntity<>(Collections.emptyList(), HttpStatus.OK);

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenReturn(response);

        assertThrows(GoogleWorkspaceSyncException.class, () -> {
            supabaseTokenService.getTokens();
        });
    }

    @Test
    void getTokens_nullBody_throwsGoogleWorkspaceSyncException() {
        ResponseEntity<List<Map<String, String>>> response = new ResponseEntity<>(null, HttpStatus.OK);

        when(restTemplate.exchange(
                anyString(),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                any(ParameterizedTypeReference.class)
        )).thenReturn(response);

        assertThrows(GoogleWorkspaceSyncException.class, () -> {
            supabaseTokenService.getTokens();
        });
    }

    @Test
    void updateTokens_success_performsPatchRequest() {
        ResponseEntity<String> response = new ResponseEntity<>("Success", HttpStatus.OK);

        when(restTemplate.exchange(
                eq("https://mock.supabase.co/rest/v1/teamleader_tokens?id=eq.1"),
                eq(HttpMethod.PATCH),
                any(HttpEntity.class),
                eq(String.class)
        )).thenReturn(response);

        supabaseTokenService.updateTokens("new-access-token", "new-refresh-token");

        verify(restTemplate, times(1)).exchange(
                eq("https://mock.supabase.co/rest/v1/teamleader_tokens?id=eq.1"),
                eq(HttpMethod.PATCH),
                any(HttpEntity.class),
                eq(String.class)
        );
    }
}
