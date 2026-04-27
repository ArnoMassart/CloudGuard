package com.cloudmen.cloudguard.unit.service;

import com.cloudmen.cloudguard.service.CloudguardStaffService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CloudguardStaffServiceTest {

    @Test
    void isCloudmenAdmin_emptyList_falseForAnyEmail() {
        CloudguardStaffService svc = new CloudguardStaffService("");
        assertFalse(svc.isCloudmenAdmin("a@b.com"));
        assertFalse(svc.isCloudmenAdmin(null));
    }

    @Test
    void isCloudmenAdmin_singleEmail_matchesCaseInsensitively() {
        CloudguardStaffService svc = new CloudguardStaffService("Staff@Cloudmen.com");
        assertTrue(svc.isCloudmenAdmin("staff@cloudmen.com"));
        assertTrue(svc.isCloudmenAdmin("STAFF@CLOUDMEN.COM"));
        assertFalse(svc.isCloudmenAdmin("other@cloudmen.com"));
    }

    @Test
    void isCloudmenAdmin_commaSeparated_trimsAndIgnoresBlanks() {
        CloudguardStaffService svc = new CloudguardStaffService(" a@x.com ,  , b@y.com ");
        assertTrue(svc.isCloudmenAdmin("a@x.com"));
        assertTrue(svc.isCloudmenAdmin("b@y.com"));
    }

    @Test
    void isCloudmenAdmin_nullEmail_false() {
        CloudguardStaffService svc = new CloudguardStaffService("a@b.com");
        assertFalse(svc.isCloudmenAdmin(null));
    }

    @Test
    void requireCloudmenAdmin_throws403WhenNotInList() {
        CloudguardStaffService svc = new CloudguardStaffService("allowed@x.com");
        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> svc.requireCloudmenAdmin("intruder@y.com"));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void requireCloudmenAdmin_doesNotThrowWhenInList() {
        CloudguardStaffService svc = new CloudguardStaffService("ok@x.com");
        svc.requireCloudmenAdmin("ok@x.com");
    }
}
