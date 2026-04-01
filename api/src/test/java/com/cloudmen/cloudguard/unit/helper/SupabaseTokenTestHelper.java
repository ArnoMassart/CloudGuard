package com.cloudmen.cloudguard.unit.helper;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

public class SupabaseTokenTestHelper {
    public static ResponseEntity<List<Map<String, String>>> mockValidTokenResponse(String accessToken, String refreshToken) {
        Map<String, String> row = Map.of(
                "access_token", accessToken,
                "refresh_token", refreshToken
        );
        return new ResponseEntity<>(List.of(row), HttpStatus.OK);
    }

    public static ResponseEntity<List<Map<String, String>>> mockEmptyResponse() {
        return new ResponseEntity<>(List.of(), HttpStatus.OK);
    }

    public static ResponseEntity<List<Map<String, String>>> mockNullBodyResponse() {
        return new ResponseEntity<>(null, HttpStatus.OK);
    }
}
