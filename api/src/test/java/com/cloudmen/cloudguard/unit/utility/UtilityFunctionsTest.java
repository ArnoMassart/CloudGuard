package com.cloudmen.cloudguard.unit.utility;

import com.cloudmen.cloudguard.utility.UtilityFunctions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class UtilityFunctionsTest {

    @Test
    void capitalizeWords_nullInput_returnsNull() {
        assertNull(UtilityFunctions.capitalizeWords(null));
    }

    @Test
    void capitalizeWords_emptyString_returnsEmptyString() {
        assertEquals("", UtilityFunctions.capitalizeWords(""));
    }

    @Test
    void capitalizeWords_allLowercase_capitalizesFirstLetters() {
        assertEquals("Hello World", UtilityFunctions.capitalizeWords("hello world"));
    }

    @Test
    void capitalizeWords_allUppercase_convertsRestToLowercase() {
        assertEquals("Hello World", UtilityFunctions.capitalizeWords("HELLO WORLD"));
    }

    @Test
    void capitalizeWords_mixedCase_normalizesCorrectly() {
        assertEquals("Java Is Awesome", UtilityFunctions.capitalizeWords("jAvA iS aWeSoMe"));
    }

    @Test
    void capitalizeWords_singleWord_capitalizesCorrectly() {
        assertEquals("Cloudguard", UtilityFunctions.capitalizeWords("cloudguard"));
    }

    @Test
    void capitalizeWords_multipleSpaces_removesExtraSpaces() {
        assertEquals("Hello World", UtilityFunctions.capitalizeWords("hello    world"));
    }

    @Test
    void capitalizeWords_leadingAndTrailingSpaces_trimsCorrectly() {
        assertEquals("Spring Boot", UtilityFunctions.capitalizeWords("  spring boot  "));
    }

    @Test
    void capitalizeWords_withNumbersAndSpecialChars_handlesGracefully() {
        assertEquals("User 123 Test!", UtilityFunctions.capitalizeWords("user 123 test!"));
    }
}
