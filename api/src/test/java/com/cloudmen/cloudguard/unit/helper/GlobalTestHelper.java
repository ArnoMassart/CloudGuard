package com.cloudmen.cloudguard.unit.helper;

import com.google.api.client.util.DateTime;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.context.support.ResourceBundleMessageSource;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class GlobalTestHelper {
    public static final String ADMIN = "admin@example.com";

    public static DateTime daysAgo(int days) {
        long millis = System.currentTimeMillis() - (days * 86400000L);
        return new DateTime(millis);
    }

    public static ResourceBundleMessageSource getMessageSource() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasenames("messages");
        messageSource.setDefaultEncoding(StandardCharsets.UTF_8.name());
        messageSource.setFallbackToSystemLocale(false);
        LocaleContextHolder.setLocale(Locale.ENGLISH);

        return messageSource;
    }
}
