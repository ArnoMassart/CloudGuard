package com.cloudmen.cloudguard.integration.controller;

import com.cloudmen.cloudguard.controller.NotificationFeedbackController;
import com.cloudmen.cloudguard.dto.notifications.NotificationFeedbackRequest;
import com.cloudmen.cloudguard.service.JwtService;
import com.cloudmen.cloudguard.service.notification.NotificationFeedbackService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationFeedbackControllerTest {

    static final String TOKEN = "jwt-token";

    @Mock
    NotificationFeedbackService notificationFeedbackService;
    @Mock
    JwtService jwtService;

    NotificationFeedbackController controller;

    @BeforeEach
    void setUp() {
        controller = new NotificationFeedbackController(notificationFeedbackService, jwtService);
    }

    @Test
    void getFeedbackKeys_validatesTokenAndReturnsSortedKeys() {
        when(jwtService.validateInternalToken(TOKEN)).thenReturn("user-1");
        when(notificationFeedbackService.getFeedbackKeysForUser("user-1"))
                .thenReturn(Set.of("b:2", "a:1"));

        var response = controller.getFeedbackKeys(TOKEN);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        List<String> body = response.getBody();
        assertNotNull(body);
        assertEquals(2, body.size());
        assertTrue(body.containsAll(List.of("b:2", "a:1")));
        verify(jwtService).validateInternalToken(TOKEN);
    }

    @Test
    void hasFeedback_delegatesToService() {
        when(jwtService.validateInternalToken(TOKEN)).thenReturn("user-1");
        when(notificationFeedbackService.hasFeedback("user-1", "s", "t")).thenReturn(true);

        var response = controller.hasFeedback(TOKEN, "s", "t");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(Boolean.TRUE, response.getBody());
    }

    @Test
    void submitFeedback_mapsBodyAndReturnsOk() {
        when(jwtService.validateInternalToken(TOKEN)).thenReturn("user-1");
        var req = new NotificationFeedbackRequest("users-groups", "user-control", "thanks");

        var response = controller.submitFeedback(TOKEN, req);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(notificationFeedbackService).submitFeedback(
                "user-1", "users-groups", "user-control", "thanks");
    }
}
