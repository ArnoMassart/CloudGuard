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

public class DateTimeConverter {
    private DateTimeConverter(){}

    public static String convertToTimeAgo(DateTime timeAgo) {
        long epochMillis = timeAgo.getValue();

        return convertTimeToPretty(epochMillis);
    }

    public static String convertToTimeAgo(long epochMillis) {
        return convertTimeToPretty(epochMillis);
    }

    public static LocalDate convertGoogleDateTime(DateTime googleDateTime) {
        long millis = googleDateTime.getValue();

        return Instant.ofEpochMilli(millis)
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
    }

    public static String parseWithPattern(LocalDateTime value, String pattern) {
        Locale userLocale = LocaleContextHolder.getLocale();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern).withLocale(userLocale);

        return value.format(formatter);
    }

    private static String convertTimeToPretty(long epochMillis) {
        Date date = new Date(epochMillis);

        Locale userLocale = LocaleContextHolder.getLocale();

        PrettyTime p = new PrettyTime(userLocale);

        return p.format(date);
    }
}
