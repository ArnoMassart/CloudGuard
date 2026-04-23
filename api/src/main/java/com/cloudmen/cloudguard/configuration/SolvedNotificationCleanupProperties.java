package com.cloudmen.cloudguard.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "cloudguard.notifications.solved-cleanup")
public class SolvedNotificationCleanupProperties {

    /** When true, periodically delete SOLVED projection rows older than {@link #retentionDays}. */
    private boolean enabled = true;

    /** Age in days after {@code solved_at} before a SOLVED row is removed. */
    private int retentionDays = 30;

    /** Cron for the cleanup job (default: daily at 04:00 server time). */
    private String cron = "0 0 4 * * *";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getRetentionDays() {
        return retentionDays;
    }

    public void setRetentionDays(int retentionDays) {
        this.retentionDays = retentionDays;
    }

    public String getCron() {
        return cron;
    }

    public void setCron(String cron) {
        this.cron = cron;
    }
}
