package com.cloudmen.cloudguard.unit.utility;

import com.cloudmen.cloudguard.utility.DateTimeConverter;
import com.google.api.client.util.DateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.i18n.LocaleContextHolder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DateTimeConverterTest {

    @BeforeEach
    void setUp() {
        LocaleContextHolder.setLocale(Locale.ENGLISH);
    }

    @AfterEach
    void tearDown() {
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    void convertToTimeAgo_fromDateTime_returnsPrettyTime() {
        long fiveMinutesAgo = System.currentTimeMillis() - (5 * 60 * 1000L);
        DateTime dateTime = new DateTime(fiveMinutesAgo);

        String result = DateTimeConverter.convertToTimeAgo(dateTime);

        assertTrue(result.contains("minute"));
    }

    @Test
    void convertToTimeAgo_fromEpoch_returnsPrettyTime() {
        long twoDaysAgo = System.currentTimeMillis() - (2 * 24 * 60 * 60 * 1000L);

        String result = DateTimeConverter.convertToTimeAgo(twoDaysAgo);

        assertTrue(result.contains("day"));
    }

    @Test
    void convertGoogleDateTime_returnsLocalDateInSystemZoneToLocalData() {
        long epochMillis = 1672531200000L;
        DateTime dateTime = new DateTime(epochMillis);

        LocalDate result = DateTimeConverter.convertGoogleDateTimeToLocalDate(dateTime);

        LocalDate expected = java.time.Instant.ofEpochMilli(epochMillis)
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
        assertEquals(expected, result);
    }

    @Test
    void parseWithPattern_formatsDateTimeAccordingToLocaleAndPattern_english() {
        LocalDateTime dateTime = LocalDateTime.of(2026, 4, 1, 11, 8);
        LocaleContextHolder.setLocale(Locale.ENGLISH);

        String result = DateTimeConverter.parseWithPattern(dateTime, "d MMMM yyyy, HH:mm");

        assertEquals("1 April 2026, 11:08", result);
    }

    @Test
    void parseWithPattern_formatsDateTimeAccordingToLocaleAndPattern_dutch() {
        LocalDateTime dateTime = LocalDateTime.of(2026, 4, 1, 11, 8);
        LocaleContextHolder.setLocale(new Locale("nl"));

        String result = DateTimeConverter.parseWithPattern(dateTime, "d MMMM yyyy, HH:mm");

        assertEquals("1 april 2026, 11:08", result);
    }
}
