package com.cloudmen.cloudguard.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "cloudguard.notifications.reminder")
public class NotificationReminderProperties {

    /**
     * When true, sends a weekly digest to each {@link com.cloudmen.cloudguard.domain.model.UserRole#SUPER_ADMIN}
     * for orgs that have active, critical, non-dismissed notification projection rows.
     */
    private boolean weeklyCriticalEnabled = false;

    /** Spring 6-field cron; default Monday 09:00 server time. */
    private String weeklyCriticalCron = "0 0 9 * * MON";

    public boolean isWeeklyCriticalEnabled() {
        return weeklyCriticalEnabled;
    }

    public void setWeeklyCriticalEnabled(boolean weeklyCriticalEnabled) {
        this.weeklyCriticalEnabled = weeklyCriticalEnabled;
    }

    public String getWeeklyCriticalCron() {
        return weeklyCriticalCron;
    }

    public void setWeeklyCriticalCron(String weeklyCriticalCron) {
        this.weeklyCriticalCron = weeklyCriticalCron;
    }
}
