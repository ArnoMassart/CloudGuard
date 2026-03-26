-- Migration: Rename resolved_* tables to dismissed_*
-- Run this script ONCE on existing databases that have resolved_notification tables
-- before deploying the DismissedNotification entity changes.
-- Fresh databases: skip this; Hibernate will create dismissed_notification tables.

-- MySQL/MariaDB syntax

-- 1. Rename main table
RENAME TABLE resolved_notification TO dismissed_notification;

-- 2. Rename column in main table
ALTER TABLE dismissed_notification
  CHANGE COLUMN resolved_at dismissed_at DATETIME;

-- 3. Rename actions table
RENAME TABLE resolved_notification_actions TO dismissed_notification_actions;

-- 4. Rename FK column in actions table
ALTER TABLE dismissed_notification_actions
  CHANGE COLUMN resolved_notification_id dismissed_notification_id BIGINT;
