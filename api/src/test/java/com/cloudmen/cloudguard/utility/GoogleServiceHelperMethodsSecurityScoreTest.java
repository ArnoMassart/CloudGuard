package com.cloudmen.cloudguard.utility;

import com.cloudmen.cloudguard.dto.password.SecurityScoreFactorDto;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GoogleServiceHelperMethodsSecurityScoreTest {

    @Test
    void securityScoreFactorForDetail_whenNotShown_usesZeroMaxScore() {
        SecurityScoreFactorDto f =
                GoogleServiceHelperMethods.securityScoreFactorForDetail(
                        false, "T", "D", 50, 100, "warning", false);
        assertEquals(0, f.maxScore());
        assertEquals(0, f.score());
        assertEquals("success", f.severity());
    }

    @Test
    void securityScoreFactorForDetail_whenShown_preservesValues() {
        SecurityScoreFactorDto f =
                GoogleServiceHelperMethods.securityScoreFactorForDetail(
                        true, "T", "D", 50, 100, "warning", false);
        assertEquals(100, f.maxScore());
        assertEquals(50, f.score());
        assertEquals("warning", f.severity());
    }
}
