package com.cloudmen.cloudguard.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Feature flags and scheduling for notification projection ({@code cloudguard.notifications.projection.*}).
 *
 * <ul>
 *   <li>{@code readEnabled} — when {@code true}, active notifications may be served from persisted projection rows
 *       instead of live aggregation once data exists for the organization.</li>
 *   <li>{@code syncEnabled} / {@code syncCron} — org-wide reconcile via
 *       {@link com.cloudmen.cloudguard.service.notification.NotificationProjectionScheduler}.</li>
 * </ul>
 */
@ConfigurationProperties(prefix = "cloudguard.notifications.projection")
public class NotificationProjectionProperties {
    private boolean readEnabled = true;
    private boolean syncEnabled = true;
    private String syncCron = "0 0 0 * * *";

    public boolean isReadEnabled() {
        return readEnabled;
    }

    public void setReadEnabled(boolean readEnabled) {
        this.readEnabled = readEnabled;
    }

    public boolean isSyncEnabled() {
        return syncEnabled;
    }

    public void setSyncEnabled(boolean syncEnabled) {
        this.syncEnabled = syncEnabled;
    }

    public String getSyncCron() {
        return syncCron;
    }

    public void setSyncCron(String syncCron) {
        this.syncCron = syncCron;
    }
}
