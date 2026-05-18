package com.cloudmen.cloudguard.domain.model.notification;

/**
 * Lifecycle of a {@link NotificationInstance} row relative to current Workspace snapshot and user preferences.
 */
public enum NotificationInstanceStatus {
    /** Issue currently detected or awaiting remediation in UI. */
    ACTIVE,
    /** Snapshot no longer contains this finding (projection retained for history until cleanup). */
    SOLVED,
    /** User preference disables this notification type; excluded from active lists. */
    DISABLED
}
