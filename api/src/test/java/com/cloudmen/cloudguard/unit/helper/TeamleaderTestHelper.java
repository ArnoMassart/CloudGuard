package com.cloudmen.cloudguard.unit.helper;

import com.cloudmen.cloudguard.dto.TeamleaderTokens;
import com.cloudmen.cloudguard.service.SupabaseTokenService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;

public class TeamleaderTestHelper {
    public static TeamleaderTokens createMockTokens(String accessToken, String refreshToken) {
        return new TeamleaderTokens(accessToken, refreshToken);
    }

    public static ResponseEntity<Map<String, Object>> createMockOAuthResponse(String newAccessToken, String newRefreshToken) {
        Map<String, Object> body = Map.of(
                "access_token", newAccessToken,
                "refresh_token", newRefreshToken
        );
        return new ResponseEntity<>(body, HttpStatus.OK);
    }

    public static Map<String, Object> createCompanyDetailsWithCustomField(String targetFieldId, Object fieldValue) {
        Map<String, Object> definition = Map.of("id", targetFieldId);
        Map<String, Object> customField = Map.of(
                "definition", definition,
                "value", fieldValue
        );

        return Map.of("custom_fields", List.of(customField));
    }

    public static Map<String, Object> createCompanyDetailsWithoutCustomFields() {
        return Map.of();
    }

    public static ResponseEntity<Map<String, Object>> createCompanyInfoResponse(Map<String, Object> dataMap) {
        return new ResponseEntity<>(Map.of("data", dataMap), HttpStatus.OK);
    }

    public static ResponseEntity<Map<String, Object>> createCompanyListResponse(List<Map<String, Object>> dataList) {
        return new ResponseEntity<>(Map.of("data", dataList), HttpStatus.OK);
    }
}
