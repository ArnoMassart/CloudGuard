package com.cloudmen.cloudguard.utility;

import com.google.api.client.util.DateTime;
import org.ocpsoft.prettytime.PrettyTime;
import org.springframework.context.i18n.LocaleContextHolder;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;

/**
 * A utility class for handling date and time conversions across the application. <p>
 *
 * This class acts as a bridge between Google's specific {@link DateTime} API and Java's modern {@code java.time} API.
 * It provides localized formatting capabilities, including human-readable relative time generation
 * (e.g., "3 days ago") using the PrettyTime library and the current user's Spring Locale context.
 */
public final class DateTimeConverter {
    // Prevents instantiation of this utility class
    private DateTimeConverter(){}

    /**
     * Converts a Google API {@link DateTime} object into a localized, human-readable relative time string.
     *
     * @param timeAgo the Google DateTime object to convert
     * @return a relative time string (e.g., "2 uur geleden" or "5 days ago")
     */
    public static String convertToTimeAgo(DateTime timeAgo) {
        long epochMillis = timeAgo.getValue();

        return convertTimeToPretty(epochMillis);
    }

    /**
     * Converts an epoch timestamp (in milliseconds) into a localized, human-readable relative time string.
     *
     * @param epochMillis the timestamp in milliseconds since the UNIX epoch
     * @return a relative time string
     */
    public static String convertToTimeAgo(long epochMillis) {
        return convertTimeToPretty(epochMillis);
    }

    /**
     * Converts a Google API {@link DateTime} object to a standard Java {@link LocalDate}, using the system's default
     * timezone.
     *
     * @param googleDateTime the Google DateTime object to convert
     * @return the corresponding {@link LocalDate}
     */
    public static LocalDate convertGoogleDateTimeToLocalDate(DateTime googleDateTime) {
        long millis = googleDateTime.getValue();

        return Instant.ofEpochMilli(millis)
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
    }

    /**
     * Formats an epoch timestamp (in milliseconds) into a localized string based on a specific DateTimeFormatter
     * pattern.
     *
     * @param epochMillis   the timestamp in milliseconds
     * @param pattern       the formatting pattern (e.g., "dd/MM/yyyy HH:mm")
     * @return the formatted date/time string
     */
    public static String parseWithPattern(long epochMillis, String pattern) {
        Instant instant = Instant.ofEpochMilli(epochMillis);

        LocalDateTime dateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());

        return parseWithPattern(dateTime, pattern);
    }

    /**
     * Formats a {@link LocalDateTime} into a localized string based on a specific DateTimeFormatter pattern.
     *
     * @param value     the local date/time object to format
     * @param pattern   the formatting pattern (e.g., "d MMMM yyyy")
     * @return the formatted, localized date/time string
     */
    public static String parseWithPattern(LocalDateTime value, String pattern) {
        Locale userLocale = LocaleContextHolder.getLocale();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern).withLocale(userLocale);

        return value.format(formatter);
    }

    /**
     * Internal helper to generate the relative time string using the
     * PrettyTime library based on the current context locale.
     */
    private static String convertTimeToPretty(long epochMillis) {
        Date date = new Date(epochMillis);

        Locale userLocale = LocaleContextHolder.getLocale();

        PrettyTime p = new PrettyTime(userLocale);

        return p.format(date);
    }
}
