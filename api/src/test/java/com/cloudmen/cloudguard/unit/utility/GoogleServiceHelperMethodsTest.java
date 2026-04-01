package com.cloudmen.cloudguard.unit.utility;

import com.cloudmen.cloudguard.dto.users.UserSecurityEvaluation;
import com.cloudmen.cloudguard.utility.GoogleServiceHelperMethods;
import com.google.api.client.util.DateTime;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.util.Base64;
import java.util.List;

import static com.cloudmen.cloudguard.unit.helper.GlobalTestHelper.daysAgo;
import static org.junit.jupiter.api.Assertions.*;

public class GoogleServiceHelperMethodsTest {

    @Test
    void decodePrivateKey_validPem_ReturnsPrivateKey() throws Exception {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(512);
        KeyPair kp = kpg.generateKeyPair();

        String base64Key = Base64.getEncoder().encodeToString(kp.getPrivate().getEncoded());
        String pem = "-----BEGIN PRIVATE KEY-----\n" + base64Key + "\n-----END PRIVATE KEY-----";

        PrivateKey privateKey = GoogleServiceHelperMethods.decodePrivateKey(pem);

        assertNotNull(privateKey);
        assertEquals("RSA", privateKey.getAlgorithm());
    }

    @Test
    void decodePrivateKey_invalidPem_throwsException() {
        String invalidPem = "-----BEGIN PRIVATE KEY-----invalidbase64-----END PRIVATE KEY-----";
        assertThrows(Exception.class, () -> GoogleServiceHelperMethods.decodePrivateKey(invalidPem));
    }

    @Test
    void translateRoleName_mapsStandardAndCustomRoles() {
        assertEquals("User", GoogleServiceHelperMethods.translateRoleName(null));
        assertEquals("Super Admin", GoogleServiceHelperMethods.translateRoleName("_SEED_ADMIN_ROLE"));
        assertEquals("Read Only Admin", GoogleServiceHelperMethods.translateRoleName("_READ_ONLY_ADMIN_ROLE"));
        assertEquals("Custom Role", GoogleServiceHelperMethods.translateRoleName("Custom Role"));
    }

    @Test
    void evaluateUserSecurity_activeAndCompliant_noViolations() {
        DateTime recentLogin = daysAgo(5);
        UserSecurityEvaluation eval = GoogleServiceHelperMethods.evaluateUserSecurity(true, recentLogin, true);

        assertTrue(eval.conform());
        assertTrue(eval.violationCodes().isEmpty());
    }

    @Test
    void evaluateUserSecurity_activeNo2FA_returnsViolation() {
        DateTime recentLogin = daysAgo(5);
        UserSecurityEvaluation eval = GoogleServiceHelperMethods.evaluateUserSecurity(true, recentLogin, false);

        assertFalse(eval.conform());
        assertTrue(eval.violationCodes().contains(GoogleServiceHelperMethods.VIOLATION_NO_2FA));
    }

    @Test
    void evaluateUserSecurity_activeStaleLogin_returnsViolation() {
        DateTime staleLogin = daysAgo(100);
        UserSecurityEvaluation eval = GoogleServiceHelperMethods.evaluateUserSecurity(true, staleLogin, true);

        assertFalse(eval.conform());
        assertTrue(eval.violationCodes().contains(GoogleServiceHelperMethods.VIOLATION_ACTIVITY_STALE));
    }

    @Test
    void evaluateUserSecurity_activeNeverLoggedIn_returnsViolation() {
        UserSecurityEvaluation eval = GoogleServiceHelperMethods.evaluateUserSecurity(true, null, true);

        assertFalse(eval.conform());
        assertTrue(eval.violationCodes().contains(GoogleServiceHelperMethods.VIOLATION_ACTIVITY_STALE));
    }

    @Test
    void evaluateUserSecurity_inactiveRecentLogin_returnsViolation() {
        DateTime recentLogin = daysAgo(2);
        UserSecurityEvaluation eval = GoogleServiceHelperMethods.evaluateUserSecurity(false, recentLogin, true);

        assertFalse(eval.conform());
        assertTrue(eval.violationCodes().contains(GoogleServiceHelperMethods.VIOLATION_ACTIVITY_INACTIVE_RECENT));
    }

    @Test
    void evaluateUserSecurity_inactiveStaleLogin_isCompliant() {
        DateTime staleLogin = daysAgo(15);
        UserSecurityEvaluation eval = GoogleServiceHelperMethods.evaluateUserSecurity(false, staleLogin, true);

        assertTrue(eval.conform());
        assertTrue(eval.violationCodes().isEmpty());
    }

    @Test
    void checkUserSecurityStatus_returnsConformBoolean() {
        assertTrue(GoogleServiceHelperMethods.checkUserSecurityStatus(true, daysAgo(5), true));
        assertFalse(GoogleServiceHelperMethods.checkUserSecurityStatus(true, daysAgo(5), false));
    }

    @Test
    void getPage_parsesValidAndInvalidInput() {
        assertEquals(1, GoogleServiceHelperMethods.getPage(null));
        assertEquals(1, GoogleServiceHelperMethods.getPage(""));
        assertEquals(1, GoogleServiceHelperMethods.getPage("   "));
        assertEquals(1, GoogleServiceHelperMethods.getPage("abc"));
        assertEquals(5, GoogleServiceHelperMethods.getPage("5"));
    }

    @Test
    void filterByNameOrEmail_filtersCorrectly() {
        record Dummy(String name, String email) {}
        List<Dummy> items = List.of(
                new Dummy("Alice Smith", "alice@example.com"),
                new Dummy("Bob Jones", "bob@example.com"),
                new Dummy(null, "charlie@example.com")
        );

        List<Dummy> emptyQuery = GoogleServiceHelperMethods.filterByNameOrEmail(items, "  ", Dummy::name, Dummy::email);
        assertEquals(3, emptyQuery.size());

        List<Dummy> byName = GoogleServiceHelperMethods.filterByNameOrEmail(items, "alice", Dummy::name, Dummy::email);
        assertEquals(1, byName.size());
        assertEquals("Alice Smith", byName.get(0).name());

        List<Dummy> byEmail = GoogleServiceHelperMethods.filterByNameOrEmail(items, "bob@example", Dummy::name, Dummy::email);
        assertEquals(1, byEmail.size());
        assertEquals("Bob Jones", byEmail.get(0).name());

        List<Dummy> noMatch = GoogleServiceHelperMethods.filterByNameOrEmail(items, "zebra", Dummy::name, Dummy::email);
        assertTrue(noMatch.isEmpty());
    }

    @Test
    void severity_calculatesStandardRanges() {
        assertEquals("success", GoogleServiceHelperMethods.severity(80));
        assertEquals("warning", GoogleServiceHelperMethods.severity(60));
        assertEquals("error", GoogleServiceHelperMethods.severity(40));
    }

    @Test
    void severity_calculatesReverseRanges() {
        assertEquals("error", GoogleServiceHelperMethods.severity(80, true));
        assertEquals("warning", GoogleServiceHelperMethods.severity(60, true));
        assertEquals("success", GoogleServiceHelperMethods.severity(40, true));
    }

    @Test
    void severity_colorOverride_returnsFixedColor() {
        assertEquals("blue", GoogleServiceHelperMethods.severity(80, false, "blue"));
        assertEquals("blue", GoogleServiceHelperMethods.severity(80, true, "blue"));
    }

    @Test
    void calculateWeightedScore_handlesMathAndZeroTotals() {
        assertEquals(100, GoogleServiceHelperMethods.calculateWeightedScore(0, 5, 10.0, 100));
        assertEquals(50, GoogleServiceHelperMethods.calculateWeightedScore(10, 5, 100.0, 0));
        assertEquals(33, GoogleServiceHelperMethods.calculateWeightedScore(3, 1, 100.0, 0));
    }

    @Test
    void calculateDeductionScore_handlesMathAndBounds() {
        assertEquals(100, GoogleServiceHelperMethods.calculateDeductionScore(0, 5));
        assertEquals(80, GoogleServiceHelperMethods.calculateDeductionScore(10, 2));
        assertEquals(0, GoogleServiceHelperMethods.calculateDeductionScore(10, 15));
    }
}
