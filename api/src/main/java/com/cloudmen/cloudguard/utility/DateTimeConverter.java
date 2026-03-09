package com.cloudmen.cloudguard.utility;

import com.google.api.client.util.DateTime;
import org.ocpsoft.prettytime.PrettyTime;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;

public class DateTimeConverter {
    public static String convertToTimeAgo(DateTime timeAgo) {
        long epochMillis = timeAgo.getValue();

        Date date = new Date(epochMillis);

        PrettyTime p = new PrettyTime(new Locale("nl"));

        return p.format(date);
    }

    public static LocalDate convertGoogleDateTime(DateTime googleDateTime) {
        long millis = googleDateTime.getValue();

        return Instant.ofEpochMilli(millis)
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
    }

    public static String parseWithPattern(LocalDateTime value, String pattern) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern).withLocale(new Locale("nl", "BE"));

        return value.format(formatter);
    }
}
